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
import org.evactor.model.Message
import org.evactor.model.Timeout
import org.evactor.monitor.Monitored
import org.evactor.subscribe._
import akka.actor.ActorLogging
import akka.actor.Terminated

/**
 * Abstract class all event processors should extend
 */
abstract class Processor (
    val subscriptions: List[Subscription]) 
  extends ProcessorBase
  with Subscriber 
  with Monitored
  with ActorLogging {
    
  final def receive = {
    case Message(_, _, event) => incr("process"); process(event)
    case Timeout => timeout()
    case Terminated(supervised) => handleTerminated(supervised)
    case msg => log.warning("Can't handle {}", msg)
  }

  protected def process(event: Event)
  
  protected def timeout() = {}
  
}

