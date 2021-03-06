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
import org.evactor.model.attributes.HasState
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evactor.model.{StateDeserializer, State}

/**
 * Represents a simple log event from a component
 */
@SerialVersionUID(1l)
case class LogEvent(
    val id: String,
    val timestamp: Long,
    val correlationId: String,
    val component: String,
    val client: String = "",
    val server: String = "",
    @JsonDeserialize(using=classOf[StateDeserializer])  state: State,
    val message: String)
  extends Event
  with HasMessage 
  with HasState {

  // Java friendly constructor
//  def this(id: String, timestamp: Long, correlationId: String, component: String, client: String, server: String, state: String, message: String) = {
//    this(id, timestamp, correlationId, component, client, server, State(state), message )
//  }
}
