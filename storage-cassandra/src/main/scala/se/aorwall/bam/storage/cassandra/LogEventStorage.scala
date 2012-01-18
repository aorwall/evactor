package se.aorwall.bam.storage.cassandra
import akka.actor.ActorContext
import me.prettyprint.cassandra.serializers.ObjectSerializer
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.factory.HFactory
import se.aorwall.bam.cassandra.CassandraUtil
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.cassandra.service.CassandraHostConfigurator

class LogEventStorage(val owner: ActorContext) extends EventStorage {

  private val settings = CassandraStorageExtension(owner.system)  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  private val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  val LOG_EVENT_CF = "LogEvent";
  
  def storeEvent(event: Event): Boolean = {
    
     val mutator = HFactory.createMutator(keyspace, StringSerializer.get);
     // TODO: check if event already exists
     
     
     // column family: RequestEvent
     // row key: event.name
     // column key: event id
     // column value: event object
     mutator.insert(event.name + event.id, LOG_EVENT_CF, HFactory.createColumn(event.id, event, StringSerializer.get, ObjectSerializer.get))
     true
  }
  
  def readEvents(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    List[LogEvent]() // TODO
  }
  
}
