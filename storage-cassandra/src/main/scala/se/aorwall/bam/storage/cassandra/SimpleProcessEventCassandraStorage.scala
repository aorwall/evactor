package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.events.SimpleProcessEvent
import akka.actor.ActorContext
import akka.actor.ActorSystem
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.EventRef

class SimpleProcessEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = SimpleProcessEvent
	
   def this(system: ActorSystem) = this(system, "SimpleProcessEvent")
   
	override val columnNames = List("id", "timestamp", "requests", "state", "latency")

   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new SimpleProcessEvent("", None,
	 	    get("id"),
	 	    get("timestamp").toLong,
	 	    getEventRefs(columns, "requests"),
	 		  State(get("state")),
	 		  java.lang.Long.parseLong(get("latency")))
	}
	
  	def eventToColumns(event: Event): List[(String, String)] = {		  
		event match {  
			case requestEvent: SimpleProcessEvent => 
				("id", event.id) ::
				("timestamp", event.timestamp.toString) ::
				("requests", requestEvent.requests.map(_.toString()).mkString(",")) ::
				("state", requestEvent.state.toString) ::
				("latency", String.valueOf(requestEvent.latency)) :: Nil
		}
	}  
  	
  def getEventRefs(columns: ColumnSlice[String, String], name: String): List[EventRef] = {
    if(columns.getColumnByName(name) != null) columns.getColumnByName(name).getValue().split(",").map(EventRef.fromString(_)).toList
    else List[EventRef]()
  }
  	
}
