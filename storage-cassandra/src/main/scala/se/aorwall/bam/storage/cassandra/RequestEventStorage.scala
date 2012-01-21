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

class RequestEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = RequestEvent

	def this(system: ActorSystem) = this(system, "RequestEvent")
	
	override val columnNames = List("name", "id", "timestamp", "inboundRef", "outboundRef", "state", "latency")

   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new RequestEvent(get("name"), 
			 			get("id"),
			 			java.lang.Long.parseLong(get("timestamp")),
			 			getEventRef(columns, "inboundRef"),
			 			getEventRef(columns, "outboundRef"),
			 			State(get("state")),
			 			java.lang.Long.parseLong(get("latency")))
	}
	
  	def eventToColumns(event: Event): List[(String, String)] = {	
		event match {  
			case requestEvent: RequestEvent => 
				("name", event.name) :: 
				("id", event.id) ::
				("timestamp", String.valueOf(event.timestamp)) ::
				getEventRefCol("inboundRef", requestEvent.inboundRef) :::
				getEventRefCol("outboundRef", requestEvent.outboundRef) :::
				("state", requestEvent.state.toString) ::
				("latency", String.valueOf(requestEvent.latency)) :: Nil
		}
	}  	
}
