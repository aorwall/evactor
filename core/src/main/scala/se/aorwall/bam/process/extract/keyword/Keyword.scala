package se.aorwall.bam.process.extract.keyword

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import akka.actor.Actor
import grizzled.slf4j.Logging
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
  extends ProcessorConfiguration(name, subscriptions){

  val eval = expression match {
    case MvelExpression(expr) => new MvelExpressionEvaluator(expr)
    case XPathExpression(expr) => new XPathExpressionEvaluator(expr)
    case other => 
      throw new IllegalArgumentException("Not a valid expression: " + other)
  }
    
  def extract (event: Event with HasMessage): Option[Event] = {   
    eval.execute(event) match {
      case Some(keyword) => 
        Some(event.clone(channel, Some(keyword)))
      case _ => None
    }	  
  }

  override def processor: Processor = {
    new Extractor(subscriptions, channel, extract)
  }

}
