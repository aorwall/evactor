package se.aorwall.bam.process.extract
import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import akka.actor.ActorRef
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName

/**
 * Extract information from messages
 */
class Extractor(override val name: String, val eventName: Option[String], extract: (Event with HasMessage) => Option[Event]) 
	extends Processor(name) with CheckEventName with Logging {
         
  type T = Event with HasMessage
  
  override def receive  = {
    case event: Event with HasMessage if(handlesEvent(event)) =>  process(event)
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  protected def process(event: Event with HasMessage) {
    
    extract(event) match {
      case Some(event) => {
        testActor match {
           case Some(actor: ActorRef) => actor ! event
           case None => collector ! event
        }        
      }
      case None => info(context.self + " couldn't extract anything from event: " + event)
    }
    
  }
}
