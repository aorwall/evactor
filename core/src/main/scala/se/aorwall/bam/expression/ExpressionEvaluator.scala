package se.aorwall.bam.expression
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasMessage

/**
 * 
 */
abstract class ExpressionEvaluator {
    
  def execute(event: Event with HasMessage): Option[Any]  
  
}