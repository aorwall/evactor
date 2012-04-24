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
package org.evactor.model.events

import scala.Array
import java.net.URLEncoder

/**
 * The event class all other event's should inherit.
 * 
 * It's identified by the event type (class name) and id. An event
 * can have clones with different paths.
 * 
 */

class Event (
    val channel: String,
    val category: Option[String],
    val id: String,
    val timestamp: Long) 
  extends Serializable {
  
  private val serialVersionUID = 0L
  
  /**
   * Clones the event but change the channel
   */
  def clone(newChannel: String, newCategory: Option[String]): Event =
    new Event(newChannel, newCategory, id, timestamp)

}

object EventRef {
  
  def apply(event: Event): EventRef = new EventRef(event.getClass.getSimpleName, event.id)
  
  def fromString(string: String): EventRef = string.split("://") match {
    case Array(className, id) => new EventRef(className, id)
    case _ => throw new IllegalArgumentException("Couldn't create an EventRef instance from argument: " + string)
  }
  
}

/**
 * An EventRef refers to an event by it's event type (class name) and id
 */
case class EventRef (
    val className: String,
    val id: String) {
  
  override def toString() = "%s://%s".format(className, id)

}
