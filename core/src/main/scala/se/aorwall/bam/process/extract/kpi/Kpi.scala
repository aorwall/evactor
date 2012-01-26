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

/**
 * Extracts a value from a message and creates a KPI Event object.
 * 
 * Only supports Json and takes the first occurrence of an element. 
 * Will add more functionality and support for Regex and Xpath later.
 */
class Kpi (override val name: String, val eventName: Option[String], val fieldName: String) extends ProcessorConfiguration(name: String) with Logging {
  
	def extract (event: Event with HasMessage): Option[Event] = {
	  	  
	  lazy val getKpi: (JsonParser => Option[Event]) = (jsonParser: JsonParser) => {
	     if(fieldName == jsonParser.getCurrentName) {
	       jsonParser.nextToken() 
	       try {
	      	 Some(new KpiEvent("%s/%s".format(event.name, name), event.id, event.timestamp, jsonParser.getText.toDouble))
	       } catch { case e: Throwable => warn("Error while parsing value", e); None }	      	 
	     } else if (jsonParser.nextToken() != JsonToken.END_OBJECT){
	       getKpi(jsonParser)
	     } else {
	       jsonParser.close()
	       None
	     }
	  }
	  
     val f = new JsonFactory();
	  val jp = f.createJsonParser(event.message);
	  getKpi(jp)
	}

   override def getProcessor(): Processor = {
     new Extractor(name, eventName, extract)
   }

}