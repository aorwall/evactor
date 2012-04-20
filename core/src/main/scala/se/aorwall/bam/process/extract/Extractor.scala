package se.aorwall.bam.process.extract

import akka.actor.{Actor, ActorLogging, ActorRef}
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process._
import se.aorwall.bam.expression.ExpressionEvaluator

/**
 * Extract information from messages. 
 * 
 * It will first evaluate the message in the event with an ExpressionEvaluator
 * and then create a new Event, based on the evaluated message, with an EventCreator.
 */
abstract class Extractor(
    override val subscriptions: List[Subscription], 
    val channel: String,
    val expression: String) 
  extends Processor(subscriptions) 
  with EventCreator
  with ExpressionEvaluator
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
	  
    createBean(evaluate(event), event, channel) match {
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
