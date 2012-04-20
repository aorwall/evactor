package se.aorwall.bam.expression

import java.util.HashMap
import org.codehaus.jackson.map.ObjectMapper
import org.mvel2.MVEL
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import scala.Double._
import javax.xml.xpath.XPathFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import java.io.ByteArrayInputStream
import akka.actor.ActorLogging

/**
 * Evaluate XPath Expressions. 
 * 
 * 
 */
trait XPathExpressionEvaluator extends ExpressionEvaluator with ActorLogging {
 
  val factory = XPathFactory.newInstance
  val xpath = factory.newXPath
  lazy val xpathExpr = xpath.compile(expression) 
  
  val domFactory = DocumentBuilderFactory.newInstance();
  //domFactory.setNamespaceAware(true); 
  val builder = domFactory.newDocumentBuilder(); 
  
  override def evaluate(event: Event with HasMessage): Option[String] = {
          
    try {
      val doc = builder.parse(new ByteArrayInputStream(event.message.getBytes())) //"UTF-8"
      val result = xpathExpr.evaluate(doc, XPathConstants.STRING)

      result match {
        case "" => None
        case s: String => Some(s)
        case _ => None
      }
    } catch {
      case e: Exception => log.warning("Failed to execute expression " + expression + " on " + event.message, e); None
    }
    
  }

}

case class XPathExpression (val expression: String) extends Expression