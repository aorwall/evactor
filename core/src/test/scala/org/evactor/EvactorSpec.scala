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

import org.evactor.model.events.DataEvent
import org.evactor.model.events.Event
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.events.ValueEvent
import org.evactor.model.State
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax

import EvactorSpec.id
import EvactorSpec.timestamp
import akka.actor.Actor
import akka.actor.ActorRef
import akka.testkit.TestActorRef

object EvactorSpec {
  val channel = "channel"
  val category = Some("category")
  val id = "id"
  val timestamp = 0L 
}

trait EvactorSpec extends WordSpec with MustMatchers with ShouldMatchers {
  import EvactorSpec._
  
  def createDataEvent(message: String) = new DataEvent(id, timestamp, message)
   
  def createEvent() = new Event { val id = EvactorSpec.id; val timestamp = EvactorSpec.timestamp }
  def createLogEvent(timestamp: Long, state: State) = new LogEvent(id, timestamp, "329380921309", "component", "client", "server", state, "hello")

  def createRequestEvent(timestamp: Long, inboundRef: Option[String], outboundRef: Option[String], corrId: String, comp: String, state: State, latency: Long) = 
    new RequestEvent(id, timestamp, corrId, comp, inboundRef, outboundRef, state, latency)

  
  def parseConfig(s: String) = {
      val options = ConfigParseOptions.defaults().
          setOriginDescription("test string").
          setSyntax(ConfigSyntax.CONF);
      ConfigFactory.parseString(s, options).asInstanceOf[Config]
  }
  
   

}