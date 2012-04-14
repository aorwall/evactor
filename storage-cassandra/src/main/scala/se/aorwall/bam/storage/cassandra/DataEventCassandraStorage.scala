package se.aorwall.bam.storage.cassandra

import akka.actor.ActorContext
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.Event
import org.codehaus.jackson.map.ObjectMapper

class DataEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = DataEvent

	def this(system: ActorSystem) = this(system, "DataEvent")
			
  override val columnNames = List("id", "timestamp", "message")
	   
  def eventToColumns(event: Event): List[(String, String)] = event match {  
	    case dataEvent: DataEvent => ("id", event.id) :: ("timestamp", event.timestamp.toString) :: ("message", dataEvent.message) :: Nil
	    case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
  def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	  val get = getValue(columns) _	
	  new DataEvent(
		        "",
		        None,
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("message"))
	}
}
