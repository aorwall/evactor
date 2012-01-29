package se.aorwall.bam.expression
import java.util.HashMap

import org.codehaus.jackson.map.ObjectMapper
import org.mvel2.MVEL

import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import scala.Double._

/**
 * Evaluate MVEL Expressions. Supports JSON and strings in message. XML to come...?
 * 
 * Maybe change so all events can be provided... and use abstract type instead of String
 * 
 */
class MvelExpressionEvaluator (expression: String) extends ExpressionEvaluator with Logging {
  
  val compiledExp = MVEL.compileExpression(expression); 

  def execute(event: Event with HasMessage): Option[String] = {
    
    val obj = new HashMap[String,Any]
    
    // assume json if message starts and ends with curly brackets
    val msg = if(event.message.startsWith("{") && event.message.endsWith("}")){
      val mapper = new ObjectMapper
      
      try {
      	Some(mapper.readValue(event.message, classOf[HashMap[String,Object]]))
      } catch {
        case _ => warn("Failed to map: " + event.message); None
      }
    } else {
      None
    }
              
    obj.put("name", event.name);
    obj.put("id", event.id);
    obj.put("timestamp", event.timestamp);
    
    msg match {
      case Some(map) => obj.put("message", map);
      case _ => obj.put("message", event.message)
    }
    
    val result = try {
      MVEL.executeExpression(compiledExp, obj); 
    } catch {
      case e => warn("Failed to execute expression", e); None
    }
    
    result match {
      case v: Any => Some(v.toString)
      case _ => None
    }
    
  }

}