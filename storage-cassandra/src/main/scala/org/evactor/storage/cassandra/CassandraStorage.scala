/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.storage.cassandra

import scala.collection.JavaConversions._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer
import org.joda.time.base.BaseSingleFieldPeriod
import org.joda.time._
import akka.actor.ActorContext
import akka.actor.ActorSystem
import org.evactor.model.attributes.HasState
import org.evactor.model.events.Event
import org.evactor.storage.EventStorage
import java.util.UUID
import org.evactor.model.State
import akka.event.Logging
import me.prettyprint.hector.api.mutation.Mutator
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.hector.api.beans.HColumn
import me.prettyprint.hector.api.beans.ColumnSlice
import grizzled.slf4j.Logging
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.serializers._
import org.evactor.model.attributes.HasLatency
import akka.actor.ExtendedActorSystem
import org.evactor.storage.LatencyStorage
import org.evactor.storage.StateStorage
import org.evactor.model.Message

class CassandraStorage(override val system: ActorSystem) extends EventStorage(system) with LatencyStorage with StateStorage with Logging {
    
  private val settings = CassandraStorageExtension(system)
  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  protected val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  val CHANNEL_CF = "Channel"
  val TIMELINE_CF = "Timeline"
  val EVENT_CF = "Event"
  val STATE_CF = "StateTimeline"
  val STATS_CF = "Statistics"
  val CATEGORY_CF = "Category"
  val LATENCY_CF = "Latency"

  val HOUR = "hour"
  val DAY = "day"
  val MONTH = "month"
  val YEAR = "year"
    
  val maxHourTimespan = 1000*3600*24*365
  val maxDayTimespan = 1000*3600*24*365*5
 
  val columnNames = List("name", "id", "timestamp")

  def eventExists(event: Event): Boolean = {
    val existingEvent = getColumn(EVENT_CF, event.id, "id")            
    existingEvent != null
  }
  
  // TODO: Change to Base64
  protected def createKey(channel: String, category: Option[String]): String = {
    category match {
      case Some(cat) => "%s/%s".format(channel, cat)
      case None => channel
    }
  }
  
  // TODO: Change to Base64
  protected def createKey(channel: String, category: Option[String], state: Option[State]): String = {
    (category, state) match {
      case (Some(cat), Some(s)) => "%s/%s/%s".format(channel, cat, s.name)
      case (Some(cat), None) => "%s/%s".format(channel, cat)
      case (None, Some(s)) => "%s/%s".format(channel, s.name)
      case (None, None) => channel
    }
  }
  
  protected def getColumn(cf: String, rowKey: String, colName: String): HColumn[String, String]  = 
    HFactory.createStringColumnQuery(keyspace)
            .setColumnFamily(cf)
            .setKey(rowKey)
            .setName(colName)
            .execute()
            .get()
            
  // TODO: Consistency level should be set to ONE for all writes and there should be some kind of rollback?            
  def storeMessage(message: Message): Unit = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
 
    val event = message.event
    
 	 // TODO: will need some kind of rollback if one of the inserts fails    
    val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)

    // Check if event already exist. Will abort if the event implementation differs...
    // TODO: Abort if anything in the event differs?
    val existingEventClass = getColumn(EVENT_CF, event.id, "class")
    if(existingEventClass != null && existingEventClass.getValue != event.getClass.getName){
      error("An event with id {} and a different type ({}) already exists in db, aborting", event.id, existingEventClass.getValue)
      return
    }
    
    mutator.addInsertion(event.id, EVENT_CF, HFactory.createColumn("class", event.getClass.getName, StringSerializer.get, StringSerializer.get))
    mutator.addInsertion(event.id, EVENT_CF, HFactory.createColumn("event", event, StringSerializer.get, ObjectSerializer.get))

    storeEventTimeline(mutator, event, message.channel, timeuuid)
    storeEventCounters(mutator, event, message.channel)

    message.category match {
      case Some(category) => {
        storeEventTimeline(mutator, event, createKey(message.channel, message.category), timeuuid)
        storeEventCounters(mutator, event, createKey(message.channel, message.category))
        mutator.incrementCounter(message.channel, CATEGORY_CF, category, 1)
      }
      case _ =>
    }    
    
    mutator.incrementCounter("channels", CHANNEL_CF, message.channel, 1)
    
    event match {
      case latencyEvent: Event with HasLatency => storeLatency(message.channel, message.category, latencyEvent)
      case _ => 
    }
    
    mutator.execute()
  }
  
  protected def storeEventTimeline(mutator: Mutator[String], event: Event, key: String, timeuuid: UUID): Unit = {
    
    // column family: EventTimeline
    // row key: event channel (+category)
    // column key: event timestamp
    // value: event name / event id
    // TODO: Add expiration time?
    mutator.addInsertion(key, TIMELINE_CF, HFactory.createColumn(timeuuid, event.id, UUIDSerializer.get, StringSerializer.get))
    
    // column family: EventState
    // row key: event name + state
    // column key: event timestamp
    // value: event id
    event match {
      case hasState: HasState => mutator.addInsertion("%s/%s".format(key, hasState.state.name), STATE_CF, HFactory.createColumn(timeuuid, "", UUIDSerializer.get, StringSerializer.get))
      case _ =>
    }
    
  }
  
  // TODO: Base64 here to...
  protected def storeEventCounters(mutator: Mutator[String], event: Event, key: String): Unit = {
    val time = new DateTime(event.timestamp)
   
    // column family: EventCount
    // row key: event name + state + ["year";"month":"day":"hour"]
    // column key: timestamp
    // value: counter
    event match {
      case hasState: HasState => storeEventCounters(mutator, "%s/%s".format(key, hasState.state.name), time)
      case _ => 
    }
    storeEventCounters(mutator, key, time)
  }
   
  protected def storeEventCounters(mutator: Mutator[String], key: String, time: DateTime): Unit = {
    
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val dayDate = new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
    val day = new java.lang.Long(dayDate.toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    
    mutator.incrementCounter("%s/%s".format(key, YEAR), STATS_CF, year, count)
    mutator.incrementCounter("%s/%s".format(key, MONTH), STATS_CF, month, count)
    mutator.incrementCounter("%s/%s".format(key, DAY), STATS_CF, day, count)
    mutator.incrementCounter("%s/%s".format(key, HOUR), STATS_CF, hour, count)
  }
  
  def getEvent(id: String): Option[Event] = {
     
     val eventColumn = HFactory.createColumnQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
     		.setColumnFamily(EVENT_CF)
     		.setName("event")
     		.setKey(id)
     		.execute().get
     		
    if(eventColumn == null) {
      warn("No event column found for event with id %s".format(id))
      None
    } else {
      val event = eventColumn.getValue.asInstanceOf[Event]
      Some(event)
    }
  }
  
  protected def storeLatency(channel: String, category: Option[String], event: Event with HasLatency) {
    storeLatency(channel, event)
    
    category match {
      case Some(cat) => storeLatency(createKey(channel, category), event)
      case _ =>
    }
  }
  
  protected def storeLatency(key: String, event: Event with HasLatency) {
	  val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
	  
    // column family: RequestEventLatency
    // row key: event name + state + ["year";"month":"day":"hour"]
    // column key: timestamp
    // value: counter
    val time = new DateTime(event.timestamp)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val dayDate = new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
    val day = new java.lang.Long(dayDate.toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    
    mutator.incrementCounter("%s/%s".format(key, YEAR), LATENCY_CF, year, event.latency)
    mutator.incrementCounter("%s/%s".format(key, MONTH), LATENCY_CF, month, event.latency)
    mutator.incrementCounter("%s/%s".format(key, DAY), LATENCY_CF, day, event.latency)
    mutator.incrementCounter("%s/%s".format(key, HOUR), LATENCY_CF, hour, event.latency)
	}
   
	def getLatencyStatistics(
	    channel: String, 
	    category: Option[String], 
	    fromTimestamp: Option[Long], 
	    toTimestamp: Option[Long], 
	    interval: String): (Long, List[(Long, Long)]) = {
	  
  
    (fromTimestamp, toTimestamp) match {
      case (None, None) => getLatencyStatisticsFromInterval(channel, category, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => getLatencyStatisticsFromInterval(channel, category, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => getLatencyStatisticsFromInterval(channel, category, from, to, interval)
    }
	  
	}
  
  protected def getLatencyStatisticsFromInterval(channel: String, category: Option[String], from: Long, to: Long, interval: String): (Long, List[(Long, Long)]) = {
	  val stats = readStatisticsFromInterval(channel, category, None, from, to, interval)
	  val key = createKey(channel, category)
	  	  
    val period = interval match {
    	case YEAR => Years.ONE
    	case MONTH => Months.ONE
    	case DAY => Days.ONE
    	case HOUR => Hours.ONE
    	case _ => throw new IllegalArgumentException("Couldn't handle request")
     }  

	  var fromDate = new DateTime(stats._1) 
	  
	  val sumList = stats._2.map { count =>
	     val sum = getLatency("%s/%s".format(key, interval), fromDate.toDate.getTime)
	     fromDate = fromDate.plus(period)
	     (count, sum)
	  } 
	  
	  (stats._1, sumList)
  }
  
	protected def getLatency(rowKey: String, colKey: Long): Long = {  
	  val currentCountCol = HFactory.createCounterColumnQuery(keyspace, StringSerializer.get, LongSerializer.get)
            .setColumnFamily(LATENCY_CF)
            .setKey(rowKey)
            .setName(colKey)
            .execute()
            .get()
         
    if(currentCountCol != null) currentCountCol.getValue
    else 0
	}
	
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    getEvents(channel, category, None, fromTimestamp, toTimestamp, count, start)
  }

  
  // TODO: Implement paging
  def getEvents(channel: String, category: Option[String], state: Option[State], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    val fromTimeuuid = fromTimestamp match {
      case Some(from) => TimeUUIDUtils.getTimeUUID(from)
      case None => null
    }
    val toTimeuuid = toTimestamp match {
      case Some(to) => TimeUUIDUtils.getTimeUUID(to)
      case None => null
    }
    
    val key =  createKey(channel, category, state)

    debug("Reading events with name " + key + " from " + fromTimestamp + " to " + fromTimestamp)

    val eventIds = HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, StringSerializer.get)
            .setColumnFamily(TIMELINE_CF)
            .setKey(key)
            .setRange(fromTimeuuid, toTimeuuid, true, count)
            .execute()
            .get
            .getColumns()
            .map { _.getValue match {
                    case s:String => s
                 }}.toList
                 
     val queryResult = HFactory.createMultigetSliceQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
     		.setColumnFamily(EVENT_CF)
     		.setColumnNames("event")
     		.setKeys(eventIds)
     		.execute()
     		
     val multigetSlice: Map[String, Event] = 
       queryResult
       	.get().iterator().map { 
          columns => columns.getColumnSlice.getColumnByName("event").getValue match {
        	case v:Any => {
        	   val event = v.asInstanceOf[Event]
        	   (event.id -> event)
        	}
       }}.toMap	

     for(eventId <- eventIds) yield multigetSlice(eventId)       
  }
  
  protected def getValue(columns: ColumnSlice[String, String])(name: String): String = {
    if(columns.getColumnByName(name) != null) columns.getColumnByName(name).getValue()
    else ""
  }
  
  def getEventCategories(channel: String, count: Int): List[(String, Long)] = {
    
    HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, StringSerializer.get)
          .setColumnFamily(CATEGORY_CF)
          .setKey(channel)
          .setRange(null, null, false, count)
          .execute()
          .get
          .getColumns.map ( col => (col.getName -> col.getValue.longValue)).toList
  }
  
  def getEventChannels(count: Int): List[(String, Long)] = {
    
    HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, StringSerializer.get)
          .setColumnFamily(CHANNEL_CF)
          .setKey("channels")
          .setRange(null, null, false, count)
          .execute()
          .get
          .getColumns.map ( col => (col.getName -> col.getValue.longValue)).toList
  }
	
  /**
   * Read statistics within a time span from fromTimestamp to toTimestamp
   */
  def getStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    getStatistics(channel, category, None, fromTimestamp, toTimestamp, interval);
  }
  
  def getStatistics(channel: String, category: Option[String], state: Option[State], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readStatisticsFromInterval(channel, category, state, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => readStatisticsFromInterval(channel, category, state, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readStatisticsFromInterval(channel, category, state, from, to, interval)
    }
  }
  
  def readStatisticsFromInterval(channel: String, category: Option[String], state: Option[State], _from: Long, to: Long, interval: String): (Long, List[Long]) = {
  	
    val key = createKey(channel, category, state)
    
    if(_from.compareTo(to) >= 0) throw new IllegalArgumentException("to is older than from")

    debug("Reading statistics for event with name " + key + " from " + _from + " to " + to + " with interval: " + interval)
        
    // Fix timestamp   
    val from = if(interval == HOUR && _from > 0 && (to-_from) > maxHourTimespan ){
      to - maxHourTimespan
    } else if(interval == DAY && _from > 0 && (to-_from) > maxDayTimespan ){
      to - maxDayTimespan
    } else {
      _from
    }
    
    val columns = HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, LongSerializer.get)
          .setColumnFamily(STATS_CF)
          .setKey("%s/%s".format(key, interval))
          .setRange(from, to, false, 100000)
          .execute()
          .get
          .getColumns
          
    val statsMap: Map[Long, Long] = columns.map {col => col.getName match {
            case k: java.lang.Long =>
            	col.getValue match {
	              case v: java.lang.Long => k.longValue -> v.longValue 
	              case _ => k.longValue -> 0L
            	}
            case _ => 0L -> 0L
	       } 
    } toMap
        
    if(statsMap.size > 0){
	    
	    val dateTime = if(from == 0){
	      // use timestamp from oldest event if from timestamp isn't set 
	    	val columns = HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, StringSerializer.get)
            .setColumnFamily(TIMELINE_CF)
            .setKey(createKey(channel, category))
            .setRange(null, null, false, 1)
            .execute()
            .get
            .getColumns()
            
         val timestamp = if(columns.size > 0){
           val col = getColumn(EVENT_CF, columns.head.getValue, "timestamp")
           
           if(col != null)
             col.getValue.toLong
           else 
             statsMap.keys.min //??
         } else {
           statsMap.keys.min //??
         }
	      new DateTime(timestamp)
	    } else {
	      new DateTime(from) 
	    }
	    
	    val (startDateTime, period) = interval match {
	    	case YEAR => (new DateTime(dateTime.getYear, 1, 1, 0, 0), Years.ONE)
	    	case MONTH => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, 1, 0, 0), Months.ONE)
	    	case DAY => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, dateTime.getDayOfMonth, 0, 0), Days.ONE)
	    	case HOUR => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, dateTime.getDayOfMonth, dateTime.getHourOfDay, 0), Hours.ONE)
	    	case _ => throw new IllegalArgumentException("Couldn't handle request")
	    }  
	    
	    // Shorten down timespan on DAY and HOUR intervals
	    var fromDate = startDateTime
	    
	    val timestampsBuffer = ListBuffer[Long]()
	    
	    while(fromDate.isBefore(to)){
	      timestampsBuffer.append(fromDate.toDate.getTime)
	      fromDate = fromDate.plus(period)
	    }
	    
	    if(timestampsBuffer.size > 0){
	   	 val timestamps = timestampsBuffer.toList   
	    	(timestamps.head, timestamps.map(timestamp => statsMap.getOrElse(timestamp, 0L)))     
	    } else {
	      (0, List())
	    }
    } else {
   	 (0, List())
    }
  }  
  
  def toMillis(date: DateTime) = date.toDate.getTime

  
}