package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import akka.actor.ActorContext
import se.aorwall.bam.storage.EventStorage
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.EventRef
import org.joda.time.DateTime
import se.aorwall.bam.model.events.KpiEvent
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.serializers.StringSerializer
import se.aorwall.bam.storage.RequestEventStorage
import org.joda.time.Years
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Months
import me.prettyprint.cassandra.serializers.LongSerializer

class RequestEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with RequestEventStorage {
	type EventType = RequestEvent

	def this(system: ActorSystem) = this(system, "RequestEvent")
	
	val LATENCY_CF = "RequestEventLatency"
	
	override val columnNames = List("id", "timestamp", "inboundRef", "outboundRef", "state", "latency")

	override def storeEvent(event: Event): Unit = event match {
	  case reqEvent: RequestEvent => { 
		  super.storeEvent(event)
		  
		  // store latency
		  storeLatency(event.channel, reqEvent)
		  
		  if(event.category.isDefined)
		  	storeLatency(createKey(event.channel, event.category), reqEvent)  
	  }
	  case msg => warn("not a RequestEvent: " + msg); false
	  
	}
	
	protected def storeLatency(key: String, event: RequestEvent) {
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
	
  def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	  val get = getValue(columns) _	
	  new RequestEvent(
		     			"", // No channel returned from db
		     			None,	 		
				 			get("id"),
				 			get("timestamp").toLong,
				 			getEventRef(columns, "inboundRef"),
				 			getEventRef(columns, "outboundRef"),
				 			State(get("state")),
				 			java.lang.Long.parseLong(get("latency")))
		}
	
  def eventToColumns(event: Event): List[(String, String)] = {	
		event match {  
			case requestEvent: RequestEvent => 
				("id", event.id) ::
				("timestamp", event.timestamp.toString) ::
				getEventRefCol("inboundRef", requestEvent.inboundRef) :::
				getEventRefCol("outboundRef", requestEvent.outboundRef) :::
				("state", requestEvent.state.toString) ::
				("latency", String.valueOf(requestEvent.latency)) :: Nil
		}
	}  	
}
