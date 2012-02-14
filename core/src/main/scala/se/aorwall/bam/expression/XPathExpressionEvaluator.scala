package se.aorwall.bam.expression
import java.util.HashMap
import org.codehaus.jackson.map.ObjectMapper
import org.mvel2.MVEL
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import scala.Double._
import javax.xml.xpath.XPathFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import java.io.ByteArrayInputStream

/**
 * Evaluate XPath Expressions. 
 * 
 * 
 */
class XPathExpressionEvaluator (expression: String) extends ExpressionEvaluator with Logging {
  
  val factory = XPathFactory.newInstance
  val xpath = factory.newXPath
  val expr = xpath.compile(expression) 
  
  val domFactory = DocumentBuilderFactory.newInstance();
  domFactory.setNamespaceAware(true); 
  val builder = domFactory.newDocumentBuilder(); 
  
  def execute(event: Event with HasMessage): Option[String] = {
          
    try {
      val doc = builder.parse(new ByteArrayInputStream(event.message.getBytes())) //"UTF-8"
      val result = expr.evaluate(doc, XPathConstants.STRING)

      result match {
        case "" => None
        case s: String => Some(s)
        case _ => None
      }
    } catch {
      case e: Exception => warn("Failed to execute expression " + expression + " on " + event.message, e); None
    }
    
  }

}

case class XPathExpression (val expression: String) extends Expression