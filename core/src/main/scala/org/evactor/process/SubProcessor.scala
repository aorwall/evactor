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

import org.evactor.model.events.Event
import org.evactor.model.Timeout
import akka.actor.actorRef2Scala
import akka.actor.ActorLogging
import akka.actor.Actor

/**
 * Processor used as a child to another processor. It can't subscribe to channels and
 * will only receive events (not encapsulated in a Message object) from it's parent
 * processor.
 * 
 * A SubProcessor isn't meant to be a persistent processor but should able to close itself down
 * when it's done.
 */
abstract class SubProcessor(val id: String) extends Actor with ActorLogging {
  
  final override def receive = {
    case event:Event => process(event)
    case Timeout => timeout()
    case msg => log.warning("Can't handle {}", msg)
  }

  protected def process(event: Event)
  
  protected def timeout() = {}

  
  /**
   * Inform parent when stopped. Would like to use death watch here but
   * waiting for http://www.assembla.com/spaces/akka/tickets/1901
   */
  override def postStop = {
     context.parent ! new Terminated(id)
  }
  
}
