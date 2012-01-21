package se.aorwall.bam.storage.cassandra

import scala.collection.JavaConversions._
import scala.collection.JavaConversions.asScalaBuffer
import org.joda.time.DateTime
import akka.actor.ActorContext
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import me.prettyprint.cassandra.serializers._
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.beans.ColumnSlice
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.EventRef
import org.joda.time.Years
import se.aorwall.bam.storage.EventStorage
import org.joda.time.Days
import se.aorwall.bam.storage.EventStorage
import org.joda.time.Hours
import org.joda.time.Months
import org.joda.time.Period
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.storage.EventStorage
import org.joda.time.base.BaseSingleFieldPeriod

abstract class CassandraStorage(val system: ActorSystem, prefix: String) extends EventStorage with Logging{
    
  private val settings = CassandraStorageExtension(system)
  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  protected val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  type EventType <: Event
  
  val TIMELINE_CF = "%sTimeline".format(prefix)
  val EVENT_CF = prefix
  val STATE_CF = "%sState".format(prefix)
  val COUNT_CF = "%sCount".format(prefix)

  val HOUR = "hour"
  val DAY = "day"
  val MONTH = "month"
  val YEAR = "year"
    
  val columnNames = List("name", "id", "timestamp")

  // TODO: Consistency level should be set to ONE for all writes
  def storeEvent(event: Event): Boolean = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    
	 val key = getKey(event)
	    
    val existingEvent = HFactory.createColumnQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
            .setColumnFamily(EVENT_CF)
            .setKey(key)
            .setName("event")
            .execute()
            .get()

    if (existingEvent != null){
      warn("An event with name %s and id %s already exists".format(event.name, event.id));
      false
    } else {
    	 // TODO: will need some kind of rollback if one of the inserts fails    
	    val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)
	    
	    // column family: EventTimeline
	    // row key: event name
	    // column key: event timestamp
	    // value: event name / event id
	    // TODO: Add expiration time?
	    mutator.addInsertion(event.name, TIMELINE_CF, HFactory.createColumn(timeuuid, key, UUIDSerializer.get, StringSerializer.get))
	
	    // column family: Event
	    // row key: event name + event.id
	    for(column <- eventToColumns(event)) {
	      mutator.addInsertion(key, EVENT_CF, column match {
	      	case (k, v) => HFactory.createStringColumn(k, v)
	      })
	    }
	    	    
	    // column family: EventState
	    // row key: event name + state
	    // column key: event timestamp
	    // value: event id
	    event match {
	      case hasState: HasState => mutator.addInsertion("%s/%s".format(event.name, hasState.state.name), STATE_CF, HFactory.createColumn(timeuuid, "", UUIDSerializer.get, StringSerializer.get))
	      case _ =>
	    }	    
	    mutator.execute()
	    
	    // column family: EventCount
	    // row key: event name + state + ["year";"month":"day":"hour"]
	    // column key: timestamp
	    // value: counter
	    val time = new DateTime(event.timestamp)
	    val count = new java.lang.Long(1L)
	    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
	    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
	    val day = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0).toDate.getTime)
	    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
	    
	    val name = event match {
	      case hasState: HasState => "%s/%s".format(event.name, hasState.state.name)
	      case _ => event.name
	    }
	    
	    mutator.incrementCounter("%s/%s".format(name, YEAR), COUNT_CF, year, count)
	    mutator.incrementCounter("%s/%s".format(name, MONTH), COUNT_CF, month, count)
	    mutator.incrementCounter("%s/%s".format(name, DAY), COUNT_CF, day, count)
	    mutator.incrementCounter("%s/%s".format(name, HOUR), COUNT_CF, hour, count)
	    true
    }
  }

  // TODO: Implement paging
  def readEvents(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    val fromTimeuuid = fromTimestamp match {
      case Some(from) => TimeUUIDUtils.getTimeUUID(from)
      case None => null
    }
    val toTimeuuid = toTimestamp match {
      case Some(to) => TimeUUIDUtils.getTimeUUID(to)
      case None => null
    }

    val eventIds = HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, StringSerializer.get)
            .setColumnFamily(TIMELINE_CF)
            .setKey(eventName)
            .setRange(fromTimeuuid, toTimeuuid, false, count)
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
     		     		
     val multigetSlice = queryResult.get().iterator().map { columns => columnsToEvent(columns.getColumnSlice()) match {
        			case event:Event => (getKey(event) -> event)
     		}}.toMap	
     
     for(eventId <- eventIds) yield multigetSlice(eventId)                      
  }

  def eventToColumns(event: Event): List[(String, String)]
  
  def columnsToEvent(columns: ColumnSlice[String, String]): Event
  
  protected def getValue(columns: ColumnSlice[String, String])(name: String): String = {
    if(columns.getColumnByName(name) != null) columns.getColumnByName(name).getValue()
    else ""
  }
  
  protected def getEventRef(columns: ColumnSlice[String, String], name: String): Option[EventRef] = {
    if(columns.getColumnByName(name) != null) Some(EventRef.fromString(columns.getColumnByName(name).getValue()))
    else None
  }
  	
  protected def getEventRefCol(name: String, eventRef: Option[EventRef]): List[(String, String)] = eventRef match {
	  case Some(eRef) => (name, eRef.toString) :: Nil
	  case None => Nil
   } 
   
  private def getKey(event: Event) = "%s/%s".format(event.name, event.id)
	
  /**
   * Read statistics within a time span from fromTimestamp to toTimestamp
   */
  def readStatistics(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): List[Long] =
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readStatisticsFromInterval(eventName, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => readStatisticsFromInterval(eventName, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readStatisticsFromInterval(eventName, from, to, interval)
  }
  
  def readStatisticsFromInterval(eventName: String, from: Long, to: Long, interval: String): List[Long] = {

    if(from.compareTo(to) >= 0) throw new IllegalArgumentException("to is older than from")

    debug("Reading statistics for event with name " + eventName + " from " + from + " to " + to + " with interval: " + interval)
    
    val columns = HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, LongSerializer.get)
          .setColumnFamily(COUNT_CF)
          .setKey("%s/%s".format(eventName, interval))
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
    
    // Fix timestamp
    val dateTime = if(from == 0) new DateTime(statsMap.keys.min)
   	 				 else new DateTime(from)
    val (startDateTime, period) = interval match {
    	case YEAR => (new DateTime(dateTime.getYear, 1, 1, 0, 0), Years.ONE)
    	case MONTH => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, 1, 0, 0), Months.ONE)
    	case DAY => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, dateTime.getDayOfMonth, 0, 0), Days.ONE)
    	case HOUR => (new DateTime(dateTime.getYear, dateTime.getMonthOfYear, dateTime.getDayOfMonth, dateTime.getHourOfDay, 0), Hours.ONE) 
    }  
    
    lazy val getTimestamps: (DateTime => List[Long]) = (fromDate: DateTime) =>  {	    
	    val newFromDate = fromDate.plus(period)
	    
	    if(newFromDate.isBefore(to)) {
	      newFromDate.toDate.getTime :: getTimestamps(newFromDate)
	    } else {
	      List()
	    }
    }
    
    val timestamps = startDateTime.toDate.getTime :: getTimestamps(startDateTime)
    timestamps.map(timestamp => statsMap.getOrElse(timestamp, 0L))      
  }
  
  
  def toMillis(date: DateTime) = date.toDate.getTime

  /*
  def readStatisticsFromDb(processId: String, from: Long)(dateProperty: Option[String], to: Long) = {
    new Statistics(
            readStatisticsCountFromDb(processId, State.SUCCESS, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.INTERNAL_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.BACKEND_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.CLIENT_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.TIMEOUT, dateProperty, from, to),
            0.0) //TODO: Fix average latency
  }
  */
  
}