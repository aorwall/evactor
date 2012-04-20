package se.aorwall.bam.expression

import scala.Double._
import scala.collection.JavaConversions.asJavaSet
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import org.codehaus.jackson.map.ObjectMapper
import org.mvel2.MVEL
import java.util.HashMap

/**
 * Evaluate MVEL Expressions. Supports JSON and strings in message. XML to come...?
 * 
 * Maybe change so all events can be provided... and use abstract type instead of String
 * 
 */
trait MvelExpressionEvaluator extends ExpressionEvaluator with ActorLogging {

  val expression: String
  lazy val compiledExp = MVEL.compileExpression(expression); 

  override def evaluate(event: Event with HasMessage): Option[String] = {
    
    val obj = new HashMap[String,Any]
    
    // assume json if message starts and ends with curly brackets
    val msg = if(event.message.startsWith("{") && event.message.endsWith("}")){
      val mapper = new ObjectMapper
      
      try {
      	Some(mapper.readValue(event.message, classOf[HashMap[String,Object]]))
      } catch {
        case _ => log.warning("Failed to map: " + event.message); None
      }
    } else {
      None
    }
              
    obj.put("channel", event.channel)
    obj.put("category", event.category.getOrElse(""))
    obj.put("id", event.id)
    obj.put("timestamp", event.timestamp)
    
    msg match {
      case Some(map) => obj.put("message", map)
      case _ => obj.put("message", event.message)
    }
    
    
    val result = try {
      MVEL.executeExpression(compiledExp, obj)
    } catch {
      case e => log.warning("Failed to execute expression", e); None
    }
    
    result match {
      case v: Any => Some(v.toString)
      case _ => None
    }    
  }
}

case class MvelExpression (val expression: String) extends Expression