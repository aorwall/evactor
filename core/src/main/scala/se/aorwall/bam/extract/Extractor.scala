package se.aorwall.bam.extract
import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event

/**
 * Extract information from messages
 */
class Extractor(eventName: String, extract: (Event with HasMessage) => Option[Event]) extends Actor with Logging {
        
  val collector = context.actorFor("/user/collect")
    
  def receive = {
    case event: Event with HasMessage if (event.name == eventName) => sendToExtractor(event)
    case _ => // skip
  }
  
  private def sendToExtractor(event: Event with HasMessage) {
    
    extract(event) match {
      case Some(event) => collector ! event
      case None => info(context.self + " couldn't extract anything from event: " + event)
    }    
    
  }   
}
