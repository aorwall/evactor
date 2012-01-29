package se.aorwall.bam.process.extract
import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import akka.actor.ActorRef
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus

/**
 * Extract information from messages
 */
class Extractor(override val name: String, val eventName: Option[String], extract: (Event with HasMessage) => Option[Event]) 
	extends Processor(name) with CheckEventName with ActorLogging {
         
  type T = Event with HasMessage
  
  override def receive  = {
    case event: Event with HasMessage if(handlesEvent(event)) => process(event)
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.debug("can't handle " + msg )
  }
  
  protected def process(event: Event with HasMessage) {
    
	 log.debug("will extract values from " + event )
	  
    extract(event) match {
      case Some(event) => {
        testActor match {
           case Some(actor: ActorRef) => actor ! event
           case None => ProcessorEventBus.publish(event)

        }        
      }
      case None => log.info("couldn't extract anything from event: " + event)
    }
    
  }
}
