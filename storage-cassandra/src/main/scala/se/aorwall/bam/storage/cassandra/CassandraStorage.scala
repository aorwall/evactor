package se.aorwall.bam.storage.cassandra

import scala.collection.JavaConversions._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer
import org.joda.time.base.BaseSingleFieldPeriod
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Months
import org.joda.time.Years
import akka.actor.ActorContext
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import me.prettyprint.cassandra.serializers._
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.beans.ColumnSlice
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.hector.api.mutation.Mutator
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.hector.api.beans.HColumn
import java.util.UUID

abstract class CassandraStorage(val system: ActorSystem, prefix: String) extends EventStorage with Logging{
    
  private val settings = CassandraStorageExtension(system)
  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  protected val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  type EventType <: Event
  
  val CHANNEL_CF = "EventChannel"
  val TIMELINE_CF = "%sTimeline".format(prefix)
  val EVENT_CF = prefix
  val STATE_CF = "%sState".format(prefix)
  val COUNT_CF = "%sCount".format(prefix)
  val CATEGORY_CF = "%sCategory".format(prefix)

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
      case Some(cat) => "%s:%s".format(channel, category)
      case None => channel
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
  def storeEvent(event: Event): Unit = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
 
 	 // TODO: will need some kind of rollback if one of the inserts fails    
    val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)

    // Traverse down the event path to see if a event is already saved in the event cf
    val existingEvent = getColumn(EVENT_CF, event.id, "id")
    if(existingEvent == null){
      // column family: Event
      // row key: event.id
      for(column <- eventToColumns(event)) {
        mutator.addInsertion(event.id, EVENT_CF, column match {
        	case (k: String, v: String) => HFactory.createStringColumn(k, v)
        })
      }
    }
    
    storeEventTimeline(mutator, event, event.channel, timeuuid)
    storeEventCounters(mutator, event, event.channel)
     
    event.category match {
      case Some(category) => {
        storeEventTimeline(mutator, event, createKey(event.channel, event.category), timeuuid)
        storeEventCounters(mutator, event, createKey(event.channel, event.category))
        mutator.incrementCounter(event.channel, CATEGORY_CF, category, 1)
      }
      case _ =>
    }    
    
    mutator.incrementCounter(prefix, CHANNEL_CF, event.channel, 1)
    
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
  
  protected def storeEventCounters(mutator: Mutator[String], event: Event, key: String): Unit = {
    
    // column family: EventCount
    // row key: event name + state + ["year";"month":"day":"hour"]
    // column key: timestamp
    // value: counter
    val time = new DateTime(event.timestamp)
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val dayDate = new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
    val day = new java.lang.Long(dayDate.toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    
    val name = event match {
      case hasState: HasState => "%s/%s".format(key, hasState.state.name)
      case _ => key
    }
    
    mutator.incrementCounter("%s/%s".format(name, YEAR), COUNT_CF, year, count)
    mutator.incrementCounter("%s/%s".format(name, MONTH), COUNT_CF, month, count)
    mutator.incrementCounter("%s/%s".format(name, DAY), COUNT_CF, day, count)
    mutator.incrementCounter("%s/%s".format(name, HOUR), COUNT_CF, hour, count)
  }
   
  // TODO: Implement paging
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    val fromTimeuuid = fromTimestamp match {
      case Some(from) => TimeUUIDUtils.getTimeUUID(from)
      case None => null
    }
    val toTimeuuid = toTimestamp match {
      case Some(to) => TimeUUIDUtils.getTimeUUID(to)
      case None => null
    }
    
    val key =  createKey(channel, category)
    
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
                 
     val queryResult = HFactory.createMultigetSliceQuery(keyspace, StringSerializer.get, StringSerializer.get, StringSerializer.get)
     		.setColumnFamily(EVENT_CF)
     		.setColumnNames(columnNames: _*)
     		.setKeys(eventIds)
     		.execute()
     		
     val multigetSlice: Map[String, Event] = 
       queryResult
       	.get().iterator().map { 
          columns => columnsToEvent(columns.getColumnSlice()) match {
        	case event:Event => (event.id -> event)
       }}.toMap	

     for(eventId <- eventIds) yield multigetSlice(eventId)                      
  }

  def eventToColumns(event: Event): List[(String, String)]
  
  def columnsToEvent(columns: ColumnSlice[String, String]): Event
  
  protected def getValue(columns: ColumnSlice[String, String])(name: String): String = {
    if(columns.getColumnByName(name) != null) columns.getColumnByName(name).getValue()
    else ""
  }
  
  def getEventCategories(channel: String, count: Int): Map[String, Long] = {
    
    HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, StringSerializer.get)
          .setColumnFamily(CATEGORY_CF)
          .setKey(channel)
          .setRange(null, null, false, count)
          .execute()
          .get
          .getColumns.map ( col => col.getName -> col.getValue.longValue).toMap
  }
  
  protected def getEventRef(columns: ColumnSlice[String, String], name: String): Option[EventRef] = {
    if(columns.getColumnByName(name) != null) Some(EventRef.fromString(columns.getColumnByName(name).getValue()))
    else None
  }
  	
  protected def getEventRefCol(name: String, eventRef: Option[EventRef]): List[(String, String)] = eventRef match {
	  case Some(eRef) => (name, eRef.toString) :: Nil
	  case None => Nil
   } 
	
  /**
   * Read statistics within a time span from fromTimestamp to toTimestamp
   */
  def getStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    val key = createKey(channel, category)
    
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readStatisticsFromInterval(key, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => readStatisticsFromInterval(key, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readStatisticsFromInterval(key, from, to, interval)
    }
  }
  
  def readStatisticsFromInterval(key: String, _from: Long, to: Long, interval: String): (Long, List[Long]) = {

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
          .setColumnFamily(COUNT_CF)
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
	      new DateTime(statsMap.keys.min)
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