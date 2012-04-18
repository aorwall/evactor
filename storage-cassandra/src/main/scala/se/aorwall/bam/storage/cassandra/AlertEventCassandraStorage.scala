package se.aorwall.bam.storage.cassandra

import akka.actor.ActorContext
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.Event
import org.codehaus.jackson.map.ObjectMapper
import se.aorwall.bam.model.events.AlertEvent

class AlertEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = AlertEvent

	def this(system: ActorSystem) = this(system, "AlertEvent")
			
  override val columnNames = List("id", "timestamp", "triggered", "message", "eventRef")
	   
  def eventToColumns(event: Event): List[(String, String)] = event match {  
	    case alertEvent: AlertEvent => 
	      ("id", event.id) :: 
	      ("timestamp", event.timestamp.toString) :: 
	      ("triggered", alertEvent.triggered.toString) :: 
	      ("message", alertEvent.message) :: 
	      getEventRefCol("eventRef", alertEvent.eventRef) :::	Nil
	    case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
  def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	  val get = getValue(columns) _	
	  new AlertEvent(
		        "",
		        None,
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("triggered").toBoolean,
			 			get("message"),
			 			getEventRef(columns, "eventRef"))
	}
}
