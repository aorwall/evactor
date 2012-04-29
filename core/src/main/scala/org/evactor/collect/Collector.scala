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

import akka.actor.Actor
import org.evactor.model.events.Event
import org.evactor.process.Processor
import org.evactor.storage.Storage
import org.evactor.process.ProcessorEventBus
import akka.actor.ActorLogging
import org.evactor.process.Publisher
import org.evactor.model.Message
import org.evactor.process.UseProcessorEventBus
import org.evactor.process.ProcessorEventBusExtension

//import com.twitter.ostrich.stats.Stats

/**
 * Collecting incoming events
 */
class Collector extends Actor with Storage with ActorLogging {
  
  private[this] val bus = ProcessorEventBusExtension(context.system)

  def receive = {
    case message: Message => collect(message)
    case msg => log.debug("can't handle {}", msg)
  }

  def collect(message: Message) {
   
    log.debug("collecting: {}", message)

    if(!eventExists(message.event)) {
      bus.publish(message)
    } else {
      log.warning("The event is already processed: {}", message.event) 
    }
    
  }

  private[this] def sendEvent(event: Event) {
    // send event to processors
    context.actorFor("../process") ! event    
  }
  
// TODO: Not used atm:
//  override def preStart = { 
//    Stats.setLabel(context.self.toString, "running")
//  }
//
//  override def postStop = {
//    Stats.setLabel(context.self.toString, "stopped")
//  }
}
