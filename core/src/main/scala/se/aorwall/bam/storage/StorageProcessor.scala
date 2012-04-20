package se.aorwall.bam.storage

import akka.actor.Actor
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.process.Subscriber
import akka.actor.Props
import akka.routing.RoundRobinRouter
import se.aorwall.bam.process.Subscription
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

/**
 * Stores events
 */

class StorageProcessorConf (
    override val name: String, 
    override val subscriptions: List[Subscription]) 
  extends ProcessorConfiguration (name, subscriptions) {
  
  def processor = new StorageProcessorRouter(subscriptions)
  
}

class StorageProcessorRouter (
    override val subscriptions: List[Subscription])  
  extends Processor (subscriptions) with Subscriber with ActorLogging {
  
  type T = Event
  
  val router = context.actorOf(Props[StorageProcessor].withRouter(RoundRobinRouter(nrOfInstances = 30))) //TODO: Should be configured in akka conf
  
  override def receive = {
    case event: Event => process(event)
    case msg => log.info("can't handle: {}", msg)
  }
  
  def process(event: Event) = router ! event
  
}

class StorageProcessor extends Actor with Storage with ActorLogging {
  
  override def receive = {
    case event: Event => log.debug("storing: {}", event); storeEvent(event) 
    case msg => log.info("can't handle: {}", msg)
  }
  
}
