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
package org.evactor.process.produce

import org.evactor.model.events.Event
import org.evactor.process.Processor
import org.evactor.subscribe.Subscription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import akka.actor.ActorLogging
import akka.camel.CamelMessage
import akka.camel.Oneway
import akka.camel.Producer
import org.apache.camel.Exchange
import org.evactor.subscribe.Subscriber

/**
 * Send an event in json format to a camel endpoint
 */
class CamelProducer (
    val subscriptions: List[Subscription],
    val camelEndpoint: String)
  extends Producer 
  with Subscriber
  with Oneway 
  with ActorLogging {
  
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  def endpointUri = camelEndpoint
  
//  override def receive = produce

  override def transformOutgoingMessage(msg: Any) = CamelMessage(mapper.writeValueAsString(msg), Map(Exchange.CONTENT_TYPE -> "application/json"))
  
  protected def process(event: Event) {
    // Not in use
  }

}
