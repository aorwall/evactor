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
package org.evactor.irc
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import org.apache.camel.util.jndi.JndiContext
import org.evactor.irc.camel.SendToAkka
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder
import org.evactor.model.events.DataEvent

class IrcAgent(val nick: String, val server: String, val ircChannels: String, val collector: ActorRef) extends Actor with ActorLogging {
	    
  val jndiContext = new JndiContext();
  jndiContext.bind("toAkka", new SendToAkka(collector));
	
  val camelContext = new DefaultCamelContext(jndiContext);	
  
  def receive () = {
    case d: DataEvent => collector ! d
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