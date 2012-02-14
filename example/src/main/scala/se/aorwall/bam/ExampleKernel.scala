package se.aorwall.bam

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.util.jndi.JndiContext
import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.kernel.Bootable
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.irc.camel.SendToAkka
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.extract.keyword.Keyword
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.process.extract.kpi.Kpi
import se.aorwall.bam.train.GetTrainReports
import se.aorwall.bam.irc.IrcAgent
import se.aorwall.bam.atom.AtomAgent

object ExampleKernel {
  
  def main(args: Array[String]){	
	  val hej = new ExampleKernel 
	  hej.startup()  
  }
}

class ExampleKernel extends Bootable {

	lazy val system = ActorSystem("example")
		
	val ircChannels = system.settings.config.getString("akka.bam.irc.channels")
	val nick = system.settings.config.getString("akka.bam.irc.nick")
	val server = system.settings.config.getString("akka.bam.irc.server")

	lazy val collector = system.actorOf(Props[Collector], name = "collect")
	lazy val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	
	//lazy val nettyServer = unfiltered.netty.Http(8080).plan(new DataEventAPI(system)).plan(new KpiEventAPI(system))
	
	def startup = {    
		// Start and configure 
	  	val irc = system.actorOf(Props(new IrcAgent(nick, server, ircChannels, collector)), name = "irc")
	  	val bamCommits = system.actorOf(Props(new AtomAgent("https://github.com/aorwall/bam/commits/master.atom", "github/commits/aorwall/bam", collector)), name = "bamCommits")
	  	val trv = system.actorOf(Props(new GetTrainReports(collector)), name = "trains")
	  	Thread.sleep(100)
	  	
		// Start and configure
	  	
	  	// TRAINS
	  	// categorize train arrivals by station
	  	processor ! new Keyword("station", Some(classOf[DataEvent].getSimpleName + "/train/arrival"), "message.TrafikplatsSignatur")
	  	
	  	// check delay 
		processor ! new Kpi("delay", Some(classOf[DataEvent].getSimpleName + "/train/arrival/station/*"), "(new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.AnnonseradTidpunktAnkomst).getTime() - new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.VerkligTidpunktAnkomst).getTime()) / 1000 / 60")
	  	
	  	// IRC
	  	// categorize messages to irc by nick
	  	processor ! new Keyword("nick", Some(classOf[DataEvent].getSimpleName + "/*"), "message.nick")	  	  	

		//nettyServer.run()	
		
	}

	def shutdown = {
		system.shutdown()
		
	//	nettyServer.stop()
	}

}
