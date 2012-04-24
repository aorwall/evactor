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
package org.evactor.process.build

import org.evactor.model.events.Event
import akka.actor.Actor

/**
 * Represents the builder of the event that decides when the event is ready to be created. Implementations of this trait
 * should be idempotent to handle duplicate incoming events.
 */
trait EventBuilder extends Actor {
    
  /**
   * Add event to event builder
   */
  def addEvent(event: Event)

  /**
   * Check if the event is finished
   */
  def isFinished: Boolean

  /**
   * Create activity with current state
   */
  def createEvent: Either[Throwable, Event] 

  /**
   * Clear state of activity builder
   */
  def clear()
}
