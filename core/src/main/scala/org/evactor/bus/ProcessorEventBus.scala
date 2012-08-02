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
package org.evactor.bus

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.event.ActorEventBus
import akka.event.LookupClassification
import org.evactor.model.Message
import org.evactor.subscribe.Subscription

/**
 * This is a first implementation of the event bus for only sending events
 * to subscribing processors. A processor can subscribe to either one type of
 * events ("event-classname/path/event") or all event names directly under a 
 * specified path ("event-classname/path/*"). 
 * 
 * Will be extended... 
 * 
 * It's used as an extension atm, maybe change to an actor instead to be 100%
 * thread safe...
 * 
 */*/

object ProcessorEventBusExtension extends ExtensionId[ProcessorEventBus] with ExtensionIdProvider {

  override def lookup = ProcessorEventBusExtension

  override def createExtension(system: ExtendedActorSystem) = new ProcessorEventBus
}

// TODO: Subclassifier or something...
class ProcessorEventBus extends Extension with ActorEventBus with LookupClassification { 
  
  type Event = Message
  type Classifier = Subscription
    
  val mapSize = 100
    
  protected def classify(e: Event): Classifier = new Subscription() // TODO: Not used?
    
  //protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.channel compareTo b.channel
    
  protected def publish(message: Message, subscriber: Subscriber) {
    //trace("publishing event {} to {}", event.toString, subscriber)
    subscriber ! message
  }
  
  override def publish(message: Message): Unit = {
    
    // and send to processors subscribing to all events
    val k = subscribers.valueIterator(new Subscription())
    while (k.hasNext) { publish(message, k.next()) }
    
    // send to processors who subscribed to all events on this channel
    val j = subscribers.valueIterator(new Subscription(Some(message.channel)))
    while (j.hasNext) { publish(message, j.next()) } 
    
  }
}

trait UseProcessorEventBus extends Actor {

  private[evactor] val bus = ProcessorEventBusExtension(context.system)

}

