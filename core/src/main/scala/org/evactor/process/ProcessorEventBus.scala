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
package org.evactor.process

import akka.actor._
import akka.event.ActorEventBus
import akka.event.LookupClassification
import org.evactor.model.Message

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
    
  def classify(event: Event): Classifier = new Subscription(Some(event.channel), event.category)
    
  //protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.channel compareTo b.channel
    
  protected def publish(message: Message, subscriber: Subscriber) {
    //trace("publishing event {} to {}", event.toString, subscriber)
    subscriber ! message
  }
  
  override def publish(message: Message): Unit = {     
    val sub = classify(message)
       
    // send to all subscribers who has this specific event subscription with a category is specified
    if(sub.category.isDefined){
      val i = subscribers.valueIterator(sub)
      while (i.hasNext) { publish(message, i.next()) } 
    }
       
	// send to all subscribers who subscribed to all events on this channel
    if(sub.channel.isDefined){
      val channelSub = new Subscription(sub.channel, None)
      val j = subscribers.valueIterator(channelSub)
      while (j.hasNext) { publish(message, j.next()) } 
    }
    
    // and send to all subscribers that hasn't specified 
    val k = subscribers.valueIterator(new Subscription())
    while (k.hasNext) { publish(message, k.next()) }
  }
}
    
trait UseProcessorEventBus extends Actor {

  private[process] val bus = ProcessorEventBusExtension(context.system)

}

