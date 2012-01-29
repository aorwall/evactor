package se.aorwall.bam.storage
import akka.actor.Actor
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus

/**
 * Stores events
 */
class StorageProcessor extends Actor with Storage with ActorLogging {
  
  override def receive = {
	    case event: Event => log.debug("Storing: " + event); storeEvent(event) 
	    case msg => log.info("Can't handle: " + msg)
  }
  
  override def preStart = {
    log.debug("subscribing to all events")
    ProcessorEventBus.subscribe(context.self, "")
  }
  
  override def postStop = {
    log.debug("unsubscribing to all events")
    ProcessorEventBus.unsubscribe(context.self, "")
  }
  
}