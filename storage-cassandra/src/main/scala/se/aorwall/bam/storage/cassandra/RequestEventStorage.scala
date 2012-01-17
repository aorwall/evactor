package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.events.RequestEvent
import akka.actor.ActorContext
import se.aorwall.bam.storage.EventStorage
import akka.actor.ActorSystem

class RequestEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type T = RequestEvent

	def this(system: ActorSystem) = this(system, "RequestEvent")
	
}
