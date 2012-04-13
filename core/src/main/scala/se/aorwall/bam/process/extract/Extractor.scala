package se.aorwall.bam.process.extract

import akka.actor.{Actor, ActorLogging, ActorRef}
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process._

/**
 * Extract information from messages. 
 */
class Extractor(
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val extract: (Event with HasMessage) => Option[Event]) 
  extends Processor(subscriptions) 
  with Monitored
  with Publisher
  with ActorLogging {
         
  type T = Event with HasMessage
  
  override def receive  = {
    case event: Event with HasMessage => process(event)
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.debug("can't handle " + msg )
  }
  
  override protected def process(event: Event with HasMessage) {
    
    log.debug("will extract values from {}", event )
	  
    extract(event) match {
      case Some(event) => {
        testActor match {
          case Some(actor: ActorRef) => actor ! event
          case None => publish(event)

        }        
      }
      case None => log.info("couldn't extract anything from event: {}", event)
    }
    
  }
}
