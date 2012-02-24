package se.aorwall.bam.collect

import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Processor
import se.aorwall.bam.storage.Storage
import se.aorwall.bam.process.ProcessorEventBus
import akka.actor.ActorLogging
import se.aorwall.bam.process.Publisher
import com.twitter.ostrich.stats.Stats

/**
 * Collecting incoming events
 */
class Collector extends Actor with Publisher with Storage with ActorLogging {

  def receive = {
    case event: Event => collect(event)
    case msg => log.debug("can't handle " + msg)
  }

  def collect(event: Event) {
   
    log.debug("collecting: " + event)

    if(!eventExists(event)) {
      publish(event)
    } else {
      log.warning("The event is already processed: " + event) 
    }
    
  }

  private[this] def sendEvent(event: Event) {
    // send event to processors
    context.actorFor("../process") ! event    
  }
  
  override def preStart = {
    log.debug("starting...")    
    Stats.setLabel(context.self.toString, "running")
  }

  override def postStop = {
    log.debug("stopping...")    
    Stats.setLabel(context.self.toString, "stopped")
  }
}
