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
package org.evactor
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.evactor.model.events._
import org.evactor.model.State

object BamSpec {
  val channel = "channel"
  val category = Some("category")
  val id = "id"
  val timestamp = 0L 
}

trait BamSpec extends WordSpec with MustMatchers with ShouldMatchers {
  import BamSpec._
  
  def createDataEvent(message: String) = new DataEvent(channel, category, id, timestamp, message)
   
  def createEvent() = new Event(channel, category, id, timestamp)
  def createLogEvent(timestamp: Long, state: State) = new LogEvent(channel, category, id, timestamp, "329380921309", "client", "server", state, "hello")

  def createRequestEvent(timestamp: Long, inboundRef: Option[EventRef], outboundRef: Option[EventRef], state: State, latency: Long) = 
    new RequestEvent(channel, category, id, timestamp, inboundRef, outboundRef, state, latency)

}