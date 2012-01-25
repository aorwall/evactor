package se.aorwall.bam.irc

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.util.jndi.JndiContext
import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.kernel.Bootable
import se.aorwall.bam.api.DataEventAPI
import se.aorwall.bam.api.KeywordEventAPI
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.irc.camel.SendToAkka
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.extract.keyword.Keyword

class IrcMonitorKernel extends Bootable {

	lazy val system = ActorSystem("IrcMonitor")
		
	val ircChannels = system.settings.config.getString("akka.bam.irc.channels")

	lazy val collector = system.actorOf(Props[Collector], name = "collect")
	lazy val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	lazy val context = connect() 	
	lazy val nettyServer = unfiltered.netty.Http(8080).plan(new DataEventAPI(system)).plan(new KeywordEventAPI(system))
	
	def startup = {    
		// Start and configure
		processor ! new Keyword("skip.nick", Some("##skip"), "nick")    
		context.start();    
		
		nettyServer.run()	
		
	}

	def shutdown = {
		context.stop()
		system.shutdown()
		
		nettyServer.stop()
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
				from("irc:%s@%s/?channels=%s&onJoin=false&onQuit=false&onPart=false".format(nick,server,ircChannels))
				.to("bean:toAkka");
			}
		});

		context
	}
}
