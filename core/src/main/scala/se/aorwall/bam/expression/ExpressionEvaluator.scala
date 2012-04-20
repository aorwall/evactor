package se.aorwall.bam.expression
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasMessage
import akka.actor.Actor
import se.aorwall.bam.process.extract.Extractor
import se.aorwall.bam.process.Processor

/**
 * 
 */
trait ExpressionEvaluator extends Actor { // TODO: Change Actor to Processor?
    
  val expression: String //TODO: Expression instead of String?
    
  def evaluate(event: Event with HasMessage): Option[Any] = {
    None
  }
  
}

abstract class Expression 