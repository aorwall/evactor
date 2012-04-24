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

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.EmptyLocalActorRef
import akka.actor.InternalActorRef
import org.evactor.model.events.Event

abstract class Processor (
    val subscriptions: List[Subscription]) 
  extends Actor
  with Subscriber 
  with ActorLogging {
  
  type T <: Event
  
  protected var testActor: Option[ActorRef] = None // actor used for testing
  
  def receive = {
    case event: T => process(event) // TODO: case event: T  doesn't work...
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }

  protected def process(event: T)
  
  override def preStart = {
    log.debug("subscribing to: {}", subscriptions)
    subscribe(context.self, subscriptions)
  }
  
  override def postStop = {
    log.debug("unsubscribing")
    unsubscribe(context.self, subscriptions)
  }
}

/**
 * Extended by processors that should be monitored by
 * Ostrich (https://github.com/twitter/ostrich)
 * 
 * TODO: This trait should use an extension instead to be able to
 * use other statistics libraries..
 * 
 */
trait Monitored extends Processor with ActorLogging {
  
  abstract override def preStart = {
    // set label context.self + running
//    Stats.setLabel(context.self.toString, "running") //TODO: Ostrich stuff, removed atm...
    super.preStart()
  }

  abstract override def postStop = {
    // set label context.self + stopped
//    Stats.setLabel(context.self.toString, "stopped") //TODO: Ostrich stuff, removed atm...
    super.postStop()
  }
  
  //TODO: This doesn't work, try intercepting the receive method instead if that's possible...
  
  //abstract override def receive = {
  //  case _: T => Stats.incr(context.self.toString)
    // count
    
 // }

}
