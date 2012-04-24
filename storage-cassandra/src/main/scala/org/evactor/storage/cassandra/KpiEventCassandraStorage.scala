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
import org.evactor.model.events.Event
import org.evactor.model.events.KpiEvent
import org.evactor.storage.KpiEventStorage

class KpiEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with KpiEventStorage {
	type EventType = KpiEvent

	def this(system: ActorSystem) = this(system, "KpiEvent")
			
   override val columnNames = List("id", "timestamp", "value")
	   
   val SUM_CF = "KpiEventSum"
     
   override def storeEvent(event: Event): Unit = event match {
	  case kpiEvent: KpiEvent => { 
		  super.storeEvent(event)
		  
		  // store sum
		  storeSum(event.channel, kpiEvent)
		  
		  if(event.category.isDefined)
		  	storeSum(createKey(event.channel, event.category), kpiEvent)  
	  }
	  case msg => warn("not a KpiEvent: " + msg); false
	  
	}
	
	protected def storeSum(key: String, event: KpiEvent) {
  
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

  	sum("%s/%s".format(key, YEAR), year, event)
  	sum("%s/%s".format(key, MONTH), month, event)
  	sum("%s/%s".format(key, DAY), day, event)
  	sum("%s/%s".format(key, HOUR), hour, event)
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
  def getSumStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Double)]) = {    
    
  
    (fromTimestamp, toTimestamp) match {
      case (None, None) => getSumStatisticsFromInterval(channel, category, 0, System.currentTimeMillis, interval)
      case (Some(from), None) => getSumStatisticsFromInterval(channel, category, from, System.currentTimeMillis, interval)
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => getSumStatisticsFromInterval(channel, category, from, to, interval)
    }
  }
  
  protected def getSumStatisticsFromInterval(channel: String, category: Option[String], from: Long, to: Long, interval: String): (Long, List[(Long, Double)]) = {
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
	     val sum = getSum("%s/%s".format(key, interval), fromDate.toDate.getTime)
	     fromDate = fromDate.plus(period)
	     (count, sum)
	  } 
	  
	  (stats._1, sumList)
  }
  
  
   def eventToColumns(event: Event): List[(String, String)] = event match {  
	  case kpiEvent: KpiEvent => ("id", event.id) :: ("timestamp", event.timestamp.toString) :: ("value", kpiEvent.value.toString) :: Nil
	  case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
     val get = getValue(columns) _	
	   new KpiEvent("", None,
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("value").toDouble)
	}
}
