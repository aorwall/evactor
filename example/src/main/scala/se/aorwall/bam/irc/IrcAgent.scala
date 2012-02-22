package se.aorwall.bam.irc
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import org.apache.camel.util.jndi.JndiContext
import se.aorwall.bam.irc.camel.SendToAkka
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder
import se.aorwall.bam.model.events.DataEvent

class IrcAgent(val nick: String, val server: String, val ircChannels: String, val collector: ActorRef) extends Actor with ActorLogging {
	    
  val jndiContext = new JndiContext();
  jndiContext.bind("toAkka", new SendToAkka(collector));
	
  val camelContext = new DefaultCamelContext(jndiContext);	
  
  def receive () = {
    case d: DataEvent => collector ! DataEvent
    case _ => 
  }
  
  override def preStart() {
    log.info("listening on channel %s on server %s".format(ircChannels, server))
    camelContext.addRoutes(new RouteBuilder() {
			def configure() {
				from("irc:%s@%s/?channels=%s&onJoin=false&onQuit=false&onPart=false".format(nick,server,ircChannels))
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