package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import akka.actor.ActorContext
import se.aorwall.bam.storage.EventStorage
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.events.Event

class RequestEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = RequestEvent

	def this(system: ActorSystem) = this(system, "RequestEvent")
	
	def eventToColumns(event: Event): List[(String, String)] = 	  
		List[(String,String)]() // TODO: Implement 
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = 
     new RequestEvent("","",0L,None,None,Start,0L) // TODO: Implement 
     
}
