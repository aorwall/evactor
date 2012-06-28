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
import org.evactor.model.{Timeout, Success, State, Message}
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
import org.evactor.model.attributes.HasLong
import org.evactor.storage.KpiStorage
import scala.collection.mutable.HashMap

class CassandraStorage(override val system: ActorSystem)
  extends EventStorage(system) 
  with LatencyStorage 
  with StateStorage 
  with KpiStorage 
  with Logging {
    
  private val settings = CassandraStorageExtension(system)
  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  protected val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  val CHANNEL_CF = "Channel"
  val TIMELINE_CF = "Timeline"
  val EVENT_CF = "Event"
  val STATS_CF = "Statistics"
  val INDEX_CF = "Index"
  
  // TODO: Remove these CF's
  val CATEGORY_CF = "Category"
  val LATENCY_CF = "Latency"
  val SUM_CF = "Sum"

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

    // extract index values from event, just state atm...
    val index = event match {
      case e: HasState => List(Map("state" -> e.state.toString))
      case _ => Nil
    }
    
    // add by channel 
    storeEventTimeline(mutator, event, new BasicKey(message.channel, None, None), timeuuid)
    storeEventCounters(mutator, event, new BasicKey(message.channel, None, None))
    mutator.incrementCounter("channels", CHANNEL_CF, message.channel, 1)

    // add by index
    index.foreach { idx =>
      storeEventTimeline(mutator, event, new BasicKey(message.channel, None, Some(idx)), timeuuid)
      storeEventCounters(mutator, event, new BasicKey(message.channel, None, Some(idx)))
      mutator.incrementCounter(new IndexKey(message.channel, None, idx.keys).keyValue, INDEX_CF, idx.values.toString, 1)
    }
    
    // add by category (deprecated)
    message.categories.foreach { category =>
        storeEventTimeline(mutator, event, new BasicKey(message.channel, Some(category), None), timeuuid)
        storeEventCounters(mutator, event, new BasicKey(message.channel, Some(category), None))
        mutator.incrementCounter(message.channel, CATEGORY_CF, category, 1)
        
        // add by category and index
        index.foreach { idx =>
          storeEventTimeline(mutator, event, new BasicKey(message.channel, Some(category), Some(idx)), timeuuid)
          storeEventCounters(mutator, event, new BasicKey(message.channel, Some(category), Some(idx)))
          mutator.incrementCounter(new IndexKey(message.channel, Some(category), idx.keys).keyValue, INDEX_CF, idx.values.toString, 1)
        }
    }

    // add latency (deprecated)
    event match {
      case le: Event with HasLatency with HasState if (le.state eq Success) => logger debug("Store latency") ; storeLatency(message.channel, message.categories, le)
      case _ => logger debug("Don't store latency for non-latency events")
    }
    
    mutator.execute()
  }
  
  protected def storeEventTimeline(mutator: Mutator[String], event: Event, key: CassandraKey, timeuuid: UUID): Unit = {
    
    // column family: EventTimeline
    // row key: event channel (+category)
    // column key: event timestamp
    // value: event name / event id
    // TODO: Add expiration time?
    mutator.addInsertion(key.keyValue, TIMELINE_CF, HFactory.createColumn(timeuuid, event.id, UUIDSerializer.get, StringSerializer.get))
  }
  
  protected def storeEventCounters(mutator: Mutator[String], event: Event, key: CassandraKey): Unit = {
    val time = new DateTime(event.timestamp)
    storeEventCounters(mutator, key, time)
  }
   
  protected def storeEventCounters(mutator: Mutator[String], key: CassandraKey, time: DateTime): Unit = {
    
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val dayDate = new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
    val day = new java.lang.Long(dayDate.toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    
    mutator.incrementCounter(new StatisticsKey(key, YEAR).keyValue, STATS_CF, year, count)
    mutator.incrementCounter(new StatisticsKey(key, MONTH).keyValue, STATS_CF, month, count)
    mutator.incrementCounter(new StatisticsKey(key, DAY).keyValue, STATS_CF, day, count)
    mutator.incrementCounter(new StatisticsKey(key, HOUR).keyValue, STATS_CF, hour, count)
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

  @deprecated("save average latency in events instead", "0.2") 
  protected def storeLatency(channel: String, categories: Set[String], event: Event with HasLatency) {
    storeSum(new BasicKey(channel, None, None), LATENCY_CF, event.timestamp, event.latency)
    
    categories.foreach { category =>
     storeSum(new BasicKey(channel, Some(category), None), LATENCY_CF, event.timestamp, event.latency)
    }
  }
  
  @deprecated("save average values in events instead", "0.2") 
  protected def storeLongValue(channel: String, categories: Set[String], event: Event with HasLong) {
    storeSum(new BasicKey(channel, None, None), SUM_CF, event.timestamp, event.value)
    
    categories.foreach { category =>
      storeSum(new BasicKey(channel, Some(category), None), SUM_CF, event.timestamp, event.value)
    }
  }
  
  @deprecated("save average latency in events instead", "0.2") 
  def getLatencyStatistics(
    channel: String, 
    category: Option[String], 
    fromTimestamp: Option[Long], 
    toTimestamp: Option[Long], 
    interval: String): (Long, List[(Long, Long)]) = {
  
  
    (fromTimestamp, toTimestamp) match {
      case (None, None) => getSumStatisticsFromInterval(LATENCY_CF, channel, category, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => getSumStatisticsFromInterval(LATENCY_CF, channel, category, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => getSumStatisticsFromInterval(LATENCY_CF, channel, category, from, to, interval)
    }
  
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
    
    val filter = state match {
      case Some(s) => Some(Map("state" -> s.toString))
      case _ =>None
    }
   
    val key =  new BasicKey(channel, category, filter)

    debug("Reading events with name " + key + " from " + fromTimestamp + " to " + fromTimestamp)

    val eventIds = HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, StringSerializer.get)
            .setColumnFamily(TIMELINE_CF)
            .setKey(key.keyValue)
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
       queryResult.get().iterator().map { 
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
  
  @deprecated("categories will be removed", "0.2") 
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
    
    val filter = state match {
      case Some(s) => Some(Map("state" -> s.toString))
      case _ =>None
    }
   
    val key =  new BasicKey(channel, category, filter)
    
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
          .setKey(new StatisticsKey(key, interval).keyValue)
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
            .setKey(key.keyValue)
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

  @deprecated("save average values in events instead", "0.2") 
  protected def storeSum(key: CassandraKey, cf: String, timestamp: Long, value: Long) {
  
    // row key: event name + state + ["year";"month":"day":"hour"]
    // column key: timestamp
    // value: counter
    val time = new DateTime(timestamp)
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val dayDate = new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
    val day = new java.lang.Long(dayDate.toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)

    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    mutator.incrementCounter(new StatisticsKey(key, YEAR).keyValue, cf, year, value)
    mutator.incrementCounter(new StatisticsKey(key, MONTH).keyValue, cf, month, value)
    mutator.incrementCounter(new StatisticsKey(key, DAY).keyValue, cf, day, value)
    mutator.incrementCounter(new StatisticsKey(key, HOUR).keyValue, cf, hour, value)
  }
  
  @deprecated("save average values in events instead", "0.2") 
  protected def getSum(cf: String, rowKey: String, colKey: Long): Long = {
    val currentCountCol = HFactory.createCounterColumnQuery(keyspace, StringSerializer.get, LongSerializer.get)
            .setColumnFamily(cf)
            .setKey(rowKey)
            .setName(colKey)
            .execute()
            .get()
         
    if(currentCountCol != null) currentCountCol.getValue()
    else 0
  }

  @deprecated("save average values in events instead", "0.2") 
  def getSumStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Long)]) = {    
    
    (fromTimestamp, toTimestamp) match {
      case (None, None) => getSumStatisticsFromInterval(SUM_CF, channel, category, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => getSumStatisticsFromInterval(SUM_CF, channel, category, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => getSumStatisticsFromInterval(SUM_CF, channel, category, from, to, interval)
    }
    
  }

  @deprecated("save average latency in events instead", "0.2") 
  protected def getSumStatisticsFromInterval(cf: String, channel: String, category: Option[String], from: Long, to: Long, interval: String): (Long, List[(Long, Long)]) = {
    val stats = readStatisticsFromInterval(channel, category, Some(Success), from, to, interval)
    val key = new BasicKey(channel, category, None)
    val period = interval match {
      case YEAR => Years.ONE
      case MONTH => Months.ONE
      case DAY => Days.ONE
      case HOUR => Hours.ONE
      case _ => throw new IllegalArgumentException("Couldn't handle request")
    }  

    var fromDate = new DateTime(stats._1) 
    val sumList = stats._2.map { count =>
      val sum = getSum(cf, new StatisticsKey(key, interval).keyValue, fromDate.toDate.getTime)
      fromDate = fromDate.plus(period)
      (count, sum)
    }
    
    (stats._1, sumList)
  }
  
  // TODO: Make use of the statistics CF to count faster
  def count(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long]): Long =
    count(channel, category, None, fromTimestamp, toTimestamp)
    
  def count(channel: String, category: Option[String], state: Option[State], fromTimestamp: Option[Long], toTimestamp: Option[Long]): Long = {
    val fromTimeuuid = fromTimestamp match {
      case Some(from) => TimeUUIDUtils.getTimeUUID(from)
      case None => null
    }
    val toTimeuuid = toTimestamp match {
      case Some(to) => TimeUUIDUtils.getTimeUUID(to)
      case None => null
    }
    
    val filter = state match {
      case Some(s) => Some(Map("state" -> s.toString))
      case _ =>None
    }
   
    val key =  new BasicKey(channel, category, filter)
    
    return HFactory.createCountQuery(keyspace, StringSerializer.get, UUIDSerializer.get)
            .setColumnFamily(TIMELINE_CF)
            .setKey(key.keyValue)
            .setRange(fromTimeuuid, toTimeuuid, 1000000)
            .execute()
            .get.toInt
  }
}