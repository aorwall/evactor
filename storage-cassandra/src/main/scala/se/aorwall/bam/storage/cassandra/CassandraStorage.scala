package se.aorwall.bam.storage.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import org.joda.time.DateTime
import grizzled.slf4j.Logging
import me.prettyprint.cassandra.serializers.ObjectSerializer
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.cassandra.serializers.UUIDSerializer
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import scala.collection.JavaConversions._
import akka.actor.ActorContext
import se.aorwall.bam.storage.EventStorage
import akka.actor.ActorSystem
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.cassandra.service.CassandraHostConfigurator

abstract class CassandraStorage(val system: ActorSystem, cfPrefix: String) extends EventStorage with Logging{
    
  private val settings = CassandraStorageExtension(system)

  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  protected val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  type T <: Event
  
  val TIMELINE_CF = "Timeline"
  val EVENT_CF = ""
  val STATE_CF = "State"
  val COUNT_CF = "Count"

  val HOUR = "hour"
  val DAY = "day"
  val MONTH = "month"
  val YEAR = "year"

  // TODO: Consistency level should be set to ONE for all writes
  def storeEvent(event: Event): Boolean = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    
    val existingEvent = HFactory.createColumnQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
            .setColumnFamily(cfPrefix + EVENT_CF)
            .setKey(event.name + event.id)
            .setName("event")
            .execute()
            .get()

    if (existingEvent != null){
      warn("An event with name " + event.name + " and id " + event.id + " already exists");
      false
    } else {
    	// TODO: will need some kind of rollback if one of the inserts fails
      
	    val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)
	
	    // column family: EventTimeline
	    // row key: event name
	    // column key: event timestamp
	    // value: event id
	    // TODO: Add expiration time?
	    mutator.addInsertion(event.name, cfPrefix + TIMELINE_CF, HFactory.createColumn(timeuuid, event.name+event.id, UUIDSerializer.get, StringSerializer.get))
	
	    // column family: Event
	    // row key: event name + event.id
	    // column key: "event" // Maybe 
	    // value: event
	    mutator.addInsertion(event.name + event.id, cfPrefix + EVENT_CF, HFactory.createColumn("event", event, StringSerializer.get, ObjectSerializer.get))
	
	    // column family: EventState
	    // row key: event name + state
	    // column key: event timestamp
	    // value: event id
	    event match {
	      case hasState: HasState => mutator.addInsertion(event.name + ":" + hasState.state.name, cfPrefix + STATE_CF, HFactory.createColumn(timeuuid, "", UUIDSerializer.get, StringSerializer.get))
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
	      case hasState: HasState => event.name + ":" + hasState.state.name
	      case _ => event.name
	    }
	    
	    mutator.incrementCounter(name + ":" + YEAR, cfPrefix + COUNT_CF, year, count)
	    mutator.incrementCounter(name + ":" + MONTH, cfPrefix + COUNT_CF, month, count)
	    mutator.incrementCounter(name + ":" + DAY, cfPrefix + COUNT_CF, day, count)
	    mutator.incrementCounter(name + ":" + HOUR, cfPrefix + COUNT_CF, hour, count)
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
            .setColumnFamily(cfPrefix + TIMELINE_CF)
            .setKey(eventName)
            .setRange(fromTimeuuid, toTimeuuid, false, count)
            .execute()
            .get
            .getColumns()
            .map { _.getValue match {
                    case s:String => s
                 }}.toList
     
     val queryResult = HFactory.createMultigetSliceQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
     		.setColumnFamily(cfPrefix + EVENT_CF)
     		.setKeys(eventIds)
     		.setColumnNames("event")
     		.execute()
     		
     queryResult.get().iterator().map { _.getColumnSlice().getColumnByName("event").getValue() match {
        case event:T => event
     }}.toList    	
  }

  /**
   * Read statistics within a time span from fromTimestamp to toTimestamp
  def readStatistics(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long]): Statistics =
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readStatisticsFromInterval(processId, new DateTime(0), new DateTime(0))
      case (Some(from), None) => readStatisticsFromInterval(processId, new DateTime(from), new DateTime(System.currentTimeMillis))
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readStatisticsFromInterval(processId, new DateTime(from), new DateTime(to))
    }

  def readStatisticsFromInterval(processId: String, from: DateTime, to: DateTime): Statistics = {

    if(from.compareTo(to) >= 0) throw new IllegalArgumentException("to is older than from")

    debug("Reading statistics for process with id " + processId + " from " + from + " to " + to)

    val readFromDb = readStatisticsFromDb(processId, toMillis(from)) _

    if(toMillis(from) == 0 && toMillis(to) == 0){
      info("al")
        readFromDb(Some(YEAR), System.currentTimeMillis) // read all
    } else if(from.getMinuteOfHour > 0 ||
              from.getSecondOfMinute > 0 ||
              from.getMillisOfSecond > 0 ||
              to.getMinuteOfHour > 0 ||
              to.getSecondOfMinute > 0 ||
              to.getMillisOfSecond > 0){
       info("s")
       readFromDb(None, toMillis(to))
    } else if (from.getHourOfDay > 0 || to.getHourOfDay > 0){
      info(HOUR)
      readFromDb(Some(HOUR), toMillis(to))
    } else if (from.getDayOfMonth > 1 || to.getDayOfMonth > 1){
      info(DAY)
      readFromDb(Some(DAY), toMillis(to))
    } else if (from.getMonthOfYear > 1 || to.getMonthOfYear > 1){
      info(MONTH)
      readFromDb(Some(MONTH), toMillis(to))
    } else {
      info(YEAR)
      readFromDb(Some(YEAR), toMillis(to))
    }
  }

  def toMillis(date: DateTime) = date.toDate.getTime

  def readStatisticsFromDb(processId: String, from: Long)(dateProperty: Option[String], to: Long) = {
    new Statistics(
            readStatisticsCountFromDb(processId, State.SUCCESS, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.INTERNAL_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.BACKEND_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.CLIENT_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.TIMEOUT, dateProperty, from, to),
            0.0) //TODO: Fix average latency
  }

  def readStatisticsCountFromDb(processId: String, state: Int, dateProperty: Option[String], from: Long, to: Long): Long = {
    dateProperty match {
      case Some(prop) =>
        HFactory.createCounterSliceQuery(keyspace, StringSerializer.get, LongSerializer.get)
          .setColumnFamily(ACTIVITY_COUNT_CF)
          .setKey(processId + ":" + state + ":" + prop)
          .setRange(from, to, false, 1000)
          .execute()
          .get
          .getColumns
          .map{_.getValue match {
            case l: java.lang.Long => l.longValue
            case _ => 0L
          }}.sum
      case None => {
        HFactory.createCountQuery(keyspace, StringSerializer.get, UUIDSerializer.get)
          .setColumnFamily(ACTIVITY_STATE_CF)
          .setKey(processId + ":" + state)
          .setRange(TimeUUIDUtils.getTimeUUID(from), TimeUUIDUtils.getTimeUUID(to), 100000000) // how set to unlimited?
          .execute()
          .get.toLong
      }
    }
  }
  */
}