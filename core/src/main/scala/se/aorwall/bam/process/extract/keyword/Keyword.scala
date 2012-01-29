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

/**
 * Extracts a path from a message and creates a new event object of the same type
 * with the event name: [eventName]/[keyword]
 * 
 * Uses MVEL to evaluate expressions, will be extended later...
 */
class Keyword (override val name: String, val eventName: Option[String], val expression: String) extends ProcessorConfiguration(name: String){

   val eval = new MvelExpressionEvaluator(expression)
  
	def extract (event: Event with HasMessage): Option[Event] = {	  	    
     eval.execute(event) match {
       case Some(keyword) => Some(event.clone("%s/%s/%s".format(event.name, name, keyword)))
       case _ => None
     }	  
	}

   override def getProcessor(): Processor = {
     new Extractor(name, eventName, extract)
   }

}
