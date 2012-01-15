package se.aorwall.bam.storage.cassandra
import se.aorwall.bam.model.events.LogEvent
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.Keyspace
import me.prettyprint.cassandra.serializers.UUIDSerializer
import me.prettyprint.cassandra.serializers.ObjectSerializer
import se.aorwall.bam.model.events.RequestEvent

class LogEventStorage(keyspace: Keyspace) {

  val LOG_EVENT_CF = "RequestEvent";
  val LOG_EVENT_TIMELINE_CF = "RequestEventTimeline";
  val LOG_EVENT_STATE_CF = "RequestEventState";
  val LOG_EVENT_COUNT_CF = "RequestEventCount";
  
  def storeEvent(event: RequestEvent): Option[RequestEvent] = {
    
     val mutator = HFactory.createMutator(keyspace, StringSerializer.get);
     // TODO: check if event already exists
     
     
     // column family: RequestEvent
     // row key: event.name
     // column key: event id
     // column value: event object
     mutator.insert(event.name, LOG_EVENT_CF, HFactory.createColumn(event.id, event, StringSerializer.get, ObjectSerializer.get))
     Some(event)
  }
  
}