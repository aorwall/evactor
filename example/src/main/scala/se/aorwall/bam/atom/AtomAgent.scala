package se.aorwall.bam.atom
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder
import se.aorwall.bam.model.events.DataEvent
import akka.actor.ActorRef
import org.apache.camel.Exchange
import org.apache.camel.Headers
import org.apache.camel.Body
import org.apache.camel.util.jndi.JndiContext
import org.apache.abdera.model.Entry;
import akka.actor.ActorLogging
import akka.actor.Actor

class AtomAgent (val url: String, val eventName: String, val collector: ActorRef) extends Actor with ActorLogging {
    
  val jndiContext = new JndiContext();
  jndiContext.bind("toAkka", new SendToAkka (context.self, eventName));
	
  val camelContext = new DefaultCamelContext(jndiContext);	
     
  def receive () = {
    case d: DataEvent => collector ! d
    case _ => 
  }  
  
  override def preStart() {
    log.info("listening on atom feed on %s".format(url))
    camelContext.addRoutes(new RouteBuilder() {
		def configure() {
			from("atom://%s?consumer.delay=60000".format(url))
			.to("bean:toAkka");
		}
    });
      
    camelContext.start()
  }
  
  override def postStop() {	  
	  log.info("stopping...")
	  camelContext.stop()	 
   }
	  
}

class SendToAkka (val actor: ActorRef, val eventName: String)  {

	def send(exchange: Exchange){	  
	  val entry = exchange.getIn().getBody(classOf[Entry]);	  	
	   
	  actor ! new DataEvent(eventName, None, entry.getId().toASCIIString(), entry.getUpdated().getTime(), entry.toString());
	}

}