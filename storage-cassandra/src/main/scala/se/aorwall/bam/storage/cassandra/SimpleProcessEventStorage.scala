package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.events.SimpleProcessEvent
import akka.actor.ActorContext
import akka.actor.ActorSystem
import se.aorwall.bam.storage.EventStorage

class SimpleProcessEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type T = SimpleProcessEvent
	
    def this(system: ActorSystem) = this(system, "SimpleProcessEvent")
   
}
