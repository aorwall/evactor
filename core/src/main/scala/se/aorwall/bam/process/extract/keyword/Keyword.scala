package se.aorwall.bam.process.extract.keyword

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import akka.actor.Actor
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.process.extract.Extractor
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor
import se.aorwall.bam.expression.MvelExpressionEvaluator
import se.aorwall.bam.expression.Expression
import se.aorwall.bam.expression.MvelExpression
import se.aorwall.bam.expression.XPathExpression
import se.aorwall.bam.expression.XPathExpressionEvaluator
import se.aorwall.bam.process.Subscription
import se.aorwall.bam.expression.ExpressionEvaluator
import se.aorwall.bam.process.extract.EventCreator

/**
 * Extracts a expression from a message and creates a new event object of the same type
 * with that expression as category.
 * 
 * Uses MVEL to evaluate expressions, will be extended later...
 */
class Keyword (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val expression: Expression) 
  extends ProcessorConfiguration(name, subscriptions) {

  override def processor: Processor = expression match {
	    case MvelExpression(expr) => new KeywordExtractor(subscriptions, channel, expr) 
	      with MvelExpressionEvaluator
	    case XPathExpression(expr) => new KeywordExtractor(subscriptions, channel, expr) 
	      with XPathExpressionEvaluator
	    case other => 
	      throw new IllegalArgumentException("Not a valid expression: " + other)
  }
}

class KeywordExtractor(
    override val subscriptions: List[Subscription], 
    override val channel: String,
    override val expression: String)
  extends Extractor (subscriptions, channel, expression) with EventCloner {
  
}

trait EventCloner extends EventCreator {
  
  def createBean(value: Option[Any], event: Event, newChannel: String): Option[Event] = value match {
    case Some(keyword: String) => 
    		   Some(event.clone(newChannel, Some(keyword)))
    case _ => None
  }
  
}
