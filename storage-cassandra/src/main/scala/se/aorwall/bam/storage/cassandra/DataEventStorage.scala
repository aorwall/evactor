package se.aorwall.bam.storage.cassandra

import me.prettyprint.hector.api.Keyspace
import akka.actor.ActorContext
import se.aorwall.bam.storage.EventStorage
import akka.actor.ActorSystem
import se.aorwall.bam.model.events.DataEvent

class DataEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type T = DataEvent

	def this(system: ActorSystem) = this(system, "DataEvent")
	
}
