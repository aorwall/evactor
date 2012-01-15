package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.events.SimpleProcessEvent

class SimpleProcessEventStorage(keyspace: Keyspace, cfPrefix: String) extends CassandraStorage (keyspace, cfPrefix) {
	type T = SimpleProcessEvent
    def this(keyspace: Keyspace) = this(keyspace, "SimpleProcess")
   
}
