/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.util.jndi.JndiContext
import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.kernel.Bootable
import org.evactor.collect.Collector
import org.evactor.irc.camel.SendToAkka
import org.evactor.process.ProcessorHandler
import org.evactor.process.extract.keyword.Keyword
import org.evactor.model.events.DataEvent
import org.evactor.process.extract.kpi.Kpi
import org.evactor.train.GetTrainReports
import org.evactor.irc.IrcAgent
import org.evactor.atom.AtomAgent
import org.evactor.expression.MvelExpression
import org.evactor.expression.XPathExpression
import org.evactor.api.DataEventAPI
import org.evactor.api.KpiEventAPI
import org.evactor.process.Subscription
import org.evactor.api.BasePlan

object ExampleKernel {
  //test
  def main(args: Array[String]){	
    val hej = new ExampleKernel 
    hej.startup()  
  }
}

class ExampleKernel extends Bootable {

	lazy val system = ActorSystem("example")
		
	val ircChannels = system.settings.config.getString("akka.evactor.irc.channels")
	val nick = system.settings.config.getString("akka.evactor.irc.nick")
	val server = system.settings.config.getString("akka.evactor.irc.server")

	lazy val collector = system.actorOf(Props[Collector], name = "collect")
	lazy val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	
	lazy val nettyServer = unfiltered.netty.Http(8080).plan(new BasePlan(system))
	
  def startup = {    
    // Start and configure 
		val irc = system.actorOf(Props(new IrcAgent(nick, server, ircChannels, collector)), name = "irc")
//    val evactorCommits = system.actorOf(Props(new AtomAgent("https://github.com/aorwall/evactor/commits/master.atom", "github/commits/evactor", collector)), name = "evactorCommits")
//    val akkaCommits = system.actorOf(Props(new AtomAgent("https://github.com/jboner/akka/commits/master.atom", "github/commits/akka", collector)), name = "akkaCommits")
//    val cassandraCommits = system.actorOf(Props(new AtomAgent("https://github.com/apache/cassandra/commits/trunk.atom", "github/commits/cassandra", collector)), name = "cassandraCommits") 
//    val scalaCommits = system.actorOf(Props(new AtomAgent("https://github.com/scala/scala/commits/master.atom", "github/commits/scala", collector)), name = "scalaCommits")
    val trv = system.actorOf(Props(new GetTrainReports(collector)), name = "trains") 
    Thread.sleep(100)
	  	
		// Start and configure
  	
    // TRAINS
    // categorize train arrivals by station
    processor ! new Keyword("station", List(new Subscription(Some("DataEvent"), Some("train/arrival"), None)), "train/arrival/station", new MvelExpression("message.TrafikplatsSignatur"))
  	
  	// check delay 
    processor ! new Kpi("delay", List(new Subscription(Some("DataEvent"), Some("train/arrival/station"), None)), "train/arrival/delay", MvelExpression("(new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.AnnonseradTidpunktAnkomst).getTime() - new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.VerkligTidpunktAnkomst).getTime()) / 1000 / 60"))
  	
  	// IRC
  	// categorize messages to the irc channel #scala by nick
  	processor ! new Keyword("nick", List(new Subscription(Some("DataEvent"), Some("irc/#scala"), None)), "irc/nick", new MvelExpression("message.nick"))	  	  	

  	// COMMITS 
		// categorized by committer
  	//processor ! new Keyword("committer", List(new Subscription(Some("DataEvent"), Some("github/commits"), None)), "github/commits/committer", new XPathExpression("//entry/author/name")) //TODO: Extract username!
  			
    // Start Ostrich admin web service
//    val adminConfig = new AdminServiceConfig {
//      httpPort = 8888
//    }
// 
//    val runtime = RuntimeEnvironment(this, Array[String]())
//    val admin = adminConfig()(runtime)
//		
    // Start Netty HTTP server
    nettyServer.run()
  }

  def shutdown = {
    system.shutdown()
    nettyServer.stop() 
  }

}

