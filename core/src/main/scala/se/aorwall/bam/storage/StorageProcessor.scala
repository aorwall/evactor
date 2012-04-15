package se.aorwall.bam.storage
import akka.actor.Actor
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.process.Subscriber
import com.twitter.ostrich.stats.Stats
import akka.actor.Props
import akka.routing.RoundRobinRouter
import se.aorwall.bam.process.Subscription

/**
 * Stores events
 */
class StorageProcessorRouter extends Actor with Subscriber with ActorLogging {
  
  val router = context.actorOf(Props[StorageProcessor].withRouter(RoundRobinRouter(nrOfInstances = 30))) //TODO: Should be configured in akka conf
  
  override def receive = {
    case event: Event => router ! event
    case msg => log.info("can't handle: {}", msg)
  }
  
  override def preStart = {
    log.debug("subscribing to all events")
    subscribe(context.self, List(new Subscription(None, None, None)))
    Stats.setLabel(context.self.toString, "running")
  }
  
  override def postStop = {
    log.debug("unsubscribing from all events")
    unsubscribe(context.self, List(new Subscription(None, None, None)))
    Stats.setLabel(context.self.toString, "stopped")
  }
  
}

class StorageProcessor extends Actor with Storage with ActorLogging {
  
  override def receive = {
    case event: Event => log.debug("storing: {}", event); storeEvent(event) 
    case msg => log.info("can't handle: {}", msg)
  }
  
}
