package se.aorwall.bam.irc

import akka.actor.ActorSystem
import akka.actor.Props
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.process.ProcessorHandler
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.util.jndi.JndiContext
import se.aorwall.bam.irc.camel.SendToAkka
import org.apache.camel.builder.RouteBuilder
import akka.kernel.Bootable
import se.aorwall.bam.extract.keyword.Keyword

class IrcMonitorKernel extends Bootable {

	val system = ActorSystem("IrcMonitor")
	val ircChannel = system.settings.config.getString("akka.bam.irc.channel")
	val collector = system.actorOf(Props[Collector], name = "collect")
	val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	val context = connect() 

	def startup = {    
		// Start and configure
		processor ! new Keyword(ircChannel, ircChannel.replace("#", "")+".nick", "nick")    
		context.start();    
	}

	def shutdown = {
		context.stop()
		system.shutdown()
	}

	def connect() = {	  
		// IRC BOT
		val jndiContext = new JndiContext();
		jndiContext.bind("toAkka", new SendToAkka(collector));

		val context = new DefaultCamelContext(jndiContext);	
		val nick = system.settings.config.getString("akka.bam.irc.nick")
		val server = system.settings.config.getString("akka.bam.irc.server")		
		
		context.addRoutes(new RouteBuilder() {
			def configure() {
				from("irc:"+nick+"@"+server+"/"+ircChannel+"?onJoin=false&onQuit=false&onPart=false")
				.to("bean:toAkka");
			}
		});

		context
	}
}
