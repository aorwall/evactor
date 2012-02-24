package se.aorwall.bam.storage
import akka.actor.Actor
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.process.Subscriber
import com.twitter.ostrich.stats.Stats
import akka.actor.Props
import akka.routing.RoundRobinRouter

/**
 * Stores events
 */
class StorageProcessorRouter extends Actor with Subscriber with ActorLogging {
  
  val router = context.actorOf(Props[StorageProcessor].withRouter(RoundRobinRouter(nrOfInstances = 10))) //TODO: Should be configured in akka conf
  
  override def receive = {
    case event: Event => router ! event
    case msg => log.info("Can't handle: " + msg)
  }
  
  override def preStart = {
    log.debug("subscribing to all events")
    subscribe(context.self, "")
    Stats.setLabel(context.self.toString, "running")
  }
  
  override def postStop = {
    log.debug("unsubscribing to all events")
    unsubscribe(context.self, "")
    Stats.setLabel(context.self.toString, "stopped")
  }
  
}

class StorageProcessor extends Actor with Storage with ActorLogging {
  
  override def receive = {
    case event: Event => log.debug("Storing: " + event); storeEvent(event) 
    case msg => log.info("Can't handle: " + msg)
  }
  
}
