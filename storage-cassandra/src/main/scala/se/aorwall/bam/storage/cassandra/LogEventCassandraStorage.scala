package se.aorwall.bam.storage.cassandra
import akka.actor.ActorContext
import me.prettyprint.cassandra.serializers.ObjectSerializer
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.factory.HFactory
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import akka.actor.ActorSystem
import me.prettyprint.cassandra.serializers.LongSerializer

class LogEventCassandraStorage(val system: ActorSystem) extends EventStorage {

  private val settings = CassandraStorageExtension(system)  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  private val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
  
  val LOG_EVENT_CF = "LogEvent";
  val NAMES_CF = "EventNames"
  
  def eventExists(event: Event) = false //TODO
    
  def storeEvent(event: Event): Unit = {
    
     val mutator = HFactory.createMutator(keyspace, StringSerializer.get);
     // TODO: check if event already exists
          
     // column family: LogEvent
     // row key: event.name
     // column key: event id
     // column value: event object
     mutator.insert("%s/%s".format(event.name, event.id), LOG_EVENT_CF, HFactory.createColumn(event.id, event, StringSerializer.get, ObjectSerializer.get))
     
	  mutator.incrementCounter(LOG_EVENT_CF, NAMES_CF, event.name, 1)

  }
  
  def getEvents(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    List[LogEvent]() // TODO
  }
  
  def getEventNames(parent: Option[String], count: Int): Map[String, Long] = {
    Map[String, Long]() //TODO
  }
  
  def getStatistics(name: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    (0L, List[Long]())
  }
  
}
