package se.aorwall.bam.storage.cassandra

import akka.actor.ActorContext
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.Event

class DataEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = DataEvent

	def this(system: ActorSystem) = this(system, "DataEvent")
			
   override val columnNames = List("name", "id", "timestamp", "message")
	
   def eventToColumns(event: Event): List[(String, String)] = event match {  
	  case dataEvent: DataEvent => ("name", event.name) :: ("id", event.id) :: ("timestamp", String.valueOf(event.timestamp)) :: ("message", dataEvent.message) :: Nil
	  case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new DataEvent(get("name").asInstanceOf[String], 
			 			get("id").asInstanceOf[String],
			 			java.lang.Long.parseLong(get("timestamp")),
			 			get("message").asInstanceOf[String])
	}
}
