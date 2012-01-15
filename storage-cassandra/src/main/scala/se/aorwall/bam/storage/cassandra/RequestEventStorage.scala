package se.aorwall.bam.storage.cassandra

import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import se.aorwall.bam.model.events.RequestEvent

class RequestEventStorage(keyspace: Keyspace, cfPrefix: String) extends CassandraStorage (keyspace, cfPrefix) {
	type T = RequestEvent
    def this(keyspace: Keyspace) = this(keyspace, "RequestEvent")
}
