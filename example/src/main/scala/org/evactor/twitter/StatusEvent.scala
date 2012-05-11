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
package org.evactor.twitter

import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event

case class StatusEvent (
    override val id: String,
    override val timestamp: Long,
    val screenName: String,
    val message: String,
    val urls: List[String],
    val hashtags: List[String]) extends Event (id, timestamp) with HasMessage {

}