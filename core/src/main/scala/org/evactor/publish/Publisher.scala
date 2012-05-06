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
package org.evactor.publish

import akka.actor.actorRef2Scala
import org.evactor.model.events.Event
import org.evactor.model.Message
import org.evactor.bus.UseProcessorEventBus

/**
 * Trait extended by actors publishing events to the processor event bus
 */
trait Publisher extends UseProcessorEventBus {
 
  val publication: Publication
  
  def publish(event: Event) {
    publication match {
      case TestPublication(testActor) => testActor ! event
      case pub: Publication => bus.publish(new Message(pub.channel(event), pub.categories(event), event))
    }
    
  }

}
