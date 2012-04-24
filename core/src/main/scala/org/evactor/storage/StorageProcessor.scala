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
package org.evactor.storage

import org.evactor.model.events.Event;
import org.evactor.process._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.routing.RoundRobinRouter

/**
 * Stores events
 */

class StorageProcessorConf (
    override val name: String, 
    override val subscriptions: List[Subscription],
    val maxThreads: Int) 
  extends ProcessorConfiguration (name, subscriptions) {
  
  def processor = new StorageProcessorRouter(subscriptions, maxThreads)
  
}

class StorageProcessorRouter (
    override val subscriptions: List[Subscription],
    val maxThreads: Int)  
  extends Processor (subscriptions) 
  with Subscriber 
  with ActorLogging {
  
  type T = Event
  
  val router = context.actorOf(Props[StorageProcessor].withRouter(RoundRobinRouter(nrOfInstances = maxThreads)))
  
  override def receive = {
    case event: Event => process(event)
    case msg => log.info("can't handle: {}", msg)
  }
  
  def process(event: Event) = router ! event
  
}

class StorageProcessor extends Actor with Storage with ActorLogging {
  
  override def receive = {
    case event: Event => log.debug("storing: {}", event); storeEvent(event) 
    case msg => log.info("can't handle: {}", msg)
  }
  
}
