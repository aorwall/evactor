package se.aorwall.bam.storage.cassandra

import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Months
import org.joda.time.Years
import akka.actor.ActorSystem
import me.prettyprint.cassandra.serializers.DoubleSerializer
import me.prettyprint.cassandra.serializers.LongSerializer
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.beans.ColumnSlice
import me.prettyprint.hector.api.factory.HFactory
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.storage.KpiEventStorage

class KpiEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with KpiEventStorage {
	type EventType = KpiEvent

	def this(system: ActorSystem) = this(system, "KpiEvent")
			
   override val columnNames = List("name", "id", "timestamp", "message")
	   
   val SUM_CF = "KpiEventSum"
     
   override def storeEvent(event: Event): Boolean = event match {
	  case kpiEvent: KpiEvent => {
	    
	     // TODO: just save the value in KpiEventTimeline, skip KpiEvent
		  if(super.storeEvent(event)) {
		  
		  // save sum
		  
		   // column family: KpiEventCountSum
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
		    
		  	 sum("%s/%s".format(event.name, YEAR), year, kpiEvent)
		  	 sum("%s/%s".format(event.name, MONTH), month, kpiEvent)
		  	 sum("%s/%s".format(event.name, DAY), day, kpiEvent)
		  	 sum("%s/%s".format(event.name, HOUR), hour, kpiEvent)
		  	 
		  	 true //TODO
		  } else {
		    false
		  }
	  }
	  case msg => warn("not a KpiEvent: " + msg); false
	  
	}
   
	/**
	 * Count sum 
	 * 
	 * TOOD: Not thread safe!
	 */
	def sum(rowKey: String, colKey: java.lang.Long, event: KpiEvent) = {	  
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    
     val sum: java.lang.Double = getSum(rowKey, colKey) + event.value
     							                             
	  mutator.insert(rowKey, SUM_CF, HFactory.createColumn(colKey, sum, LongSerializer.get, DoubleSerializer.get))
	  
	}
	
	protected def getSum(rowKey: String, colKey: Long): Double = {
	 val currentCountCol = HFactory.createColumnQuery(keyspace, StringSerializer.get, LongSerializer.get, DoubleSerializer.get)
            .setColumnFamily(SUM_CF)
            .setKey(rowKey)
            .setName(colKey)
            .execute()
            .get()
         
     if(currentCountCol != null) currentCountCol.getValue()
     else 0.0
	}
	
  /**
   * Read kpi statistics within a time span from fromTimestamp to toTimestamp
   */
  def readSumStatistics(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Double)]) =
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readSumStatisticsFromInterval(eventName, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => readSumStatisticsFromInterval(eventName, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readSumStatisticsFromInterval(eventName, from, to, interval)
  }
  
  def readSumStatisticsFromInterval(eventName: String, from: Long, to: Long, interval: String): (Long, List[(Long, Double)]) = {
	  val stats = readStatisticsFromInterval(eventName, from, to, interval)
	  	  
     val period = interval match {
    	case YEAR => Years.ONE
    	case MONTH => Months.ONE
    	case DAY => Days.ONE
    	case HOUR => Hours.ONE
    	case _ => throw new IllegalArgumentException("Couldn't handle request")
     }  

	  var fromDate = new DateTime(stats._1) 
	  
	  val sumList = stats._2.map { count =>
	     val sum = getSum("%s/%s".format(eventName, interval), fromDate.toDate.getTime)
	     fromDate = fromDate.plus(period)
	     (count, sum)
	  } 
	  
	  (stats._1, sumList)
  }
  
  
   def eventToColumns(event: Event): List[(String, String)] = event match {  
	  case kpiEvent: KpiEvent => ("name", event.name) :: ("id", event.id) :: ("timestamp", event.timestamp.toString) :: ("value", kpiEvent.value.toString) :: Nil
	  case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new KpiEvent(get("name"), 
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("value").toDouble)
	}
}
