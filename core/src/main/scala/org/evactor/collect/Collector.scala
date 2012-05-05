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
package org.evactor.collect

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import org.evactor.model.events.Event
import org.evactor.process.Processor
import org.evactor.storage.Storage
import org.evactor.process.ProcessorEventBus
import org.evactor.process.Publisher
import org.evactor.model.Message
import org.evactor.process.UseProcessorEventBus
import org.evactor.process.ProcessorEventBusExtension
import org.evactor.process.Publication
import org.evactor.listen.Listener
import org.evactor.transform.Transformer
import org.evactor.listen.ListenerConfiguration
import org.evactor.transform.TransformerConfiguration


//import com.twitter.ostrich.stats.Stats

/**
 * Collecting incoming events
 */
class Collector(
    val listener: Option[ListenerConfiguration], 
    val transformer: Option[TransformerConfiguration], 
    val publication: Publication) 
  extends Actor 
  with Storage 
  with Publisher
  with ActorLogging {
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: IllegalArgumentException => Stop
    case _: Exception => Restart
  }
  
  def receive = {
    case event: Event => collect(event)
    case msg => log.debug("can't handle {}", msg)
  }

  def collect(event: Event) {
   
    log.debug("collecting: {}", event)

    if(!eventExists(event)) {
      publish(event)
    } else {
      log.warning("The event is already processed: {}", event) 
    }
    
  }
  
  override def preStart = { 
    // Start transformer and set collector to it
    val sendTo = transformer match {
      case Some(t) => context.actorOf(Props(t.transformer(context.self)))
      case None => context.self
    }
    
    // Start listener and set a  transformer to it
    listener match {
      case Some(l) => context.actorOf(Props(l.listener(sendTo)))
      case None => log.warning("No listener")
    }
    
  }

  override def postStop = {
    // Stop transformer and listener?
  }
}
