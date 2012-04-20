package se.aorwall.bam.process.extract.kpi
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.process.extract.Extractor
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.expression.MvelExpressionEvaluator
import se.aorwall.bam.process.Subscription
import se.aorwall.bam.process.extract.EventCreator
import se.aorwall.bam.expression.MvelExpression
import se.aorwall.bam.expression.XPathExpressionEvaluator
import se.aorwall.bam.expression.XPathExpression
import se.aorwall.bam.expression.Expression
import akka.actor.ActorLogging

/**
 * Extracts a value from a message and creates a KPI Event object. Using a specified
 * channel and the same category as the provided event if one exists
 * 
 * Uses MVEL to evaluate expressions and must return a float value, will be extended later...
 */
class Kpi (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val expression: Expression) 
  extends ProcessorConfiguration(name, subscriptions) {
   
  override def processor: Processor = expression match {
	    case MvelExpression(expr) => new KpiExtractor(subscriptions, channel, expr) 
	    	with MvelExpressionEvaluator
	    case XPathExpression(expr) => new KpiExtractor(subscriptions, channel, expr) 
	    	with XPathExpressionEvaluator
	    case other => 
	      throw new IllegalArgumentException("Not a valid expression: " + other)
  }
}

class KpiExtractor(
    override val subscriptions: List[Subscription], 
    override val channel: String,
    override val expression: String)
  extends Extractor (subscriptions, channel, expression) with KpiEventCreator {
  
}

trait KpiEventCreator extends EventCreator {
  
  def createBean(value: Option[Any], event: Event, newChannel: String): Option[Event] = value match {
    case Some(value: String) => try {
      Some(new KpiEvent(newChannel, event.category, event.id, event.timestamp, value.toDouble)) 
    } catch {
      case _ => None
    }
    case a => None
  }
    
}