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

import org.evactor.model.attributes.HasMessage

/**
 * An event with a message of some sort
 */
case class DataEvent (    
    override val channel: String, 
    override val category: Option[String],
    override val id: String, 
    override val timestamp: Long, 
    val message: String) 
  extends Event(channel, category, id, timestamp) 
  with HasMessage {

  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new DataEvent(newChannel, newCategory, id, timestamp, message)
  
}
