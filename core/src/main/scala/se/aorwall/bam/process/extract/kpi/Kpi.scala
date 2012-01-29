package se.aorwall.bam.process.extract.kpi
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.process.extract.Extractor
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.expression.MvelExpressionEvaluator

/**
 * Extracts a value from a message and creates a KPI Event object.
 * 
 * Uses MVEL to evaluate expressions and must return a float value, will be extended later...
 */
class Kpi (override val name: String, val eventName: Option[String], val expression: String) extends ProcessorConfiguration(name: String) with Logging {
   
  val eval = new MvelExpressionEvaluator(expression) // Must fix so MvelExpressionEvaluator always returns a double
   
	def extract (event: Event with HasMessage): Option[Event] = {

    eval.execute(event) match {
       case Some(value: String) => try {
         Some(new KpiEvent("%s/%s".format(event.name, name), event.id, event.timestamp, value.toDouble)) 
       } catch {
         case _ => None
       }
       case a => None
     }
	  	
	}

   override def getProcessor(): Processor = {
     new Extractor(name, eventName, extract)
   }

}