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
import akka.actor.ActorLogging
import akka.event.Logging
import org.evactor.ConfigurationException
import org.evactor.model.Message
import akka.camel.Oneway
import akka.camel.Producer

class CamelProducer (
    override val subscriptions: List[Subscription],
    val camelEndpoint: String)
  extends Processor (subscriptions) 
  with Producer 
  with Oneway 
  with ActorLogging {
  
  def endpointUri = camelEndpoint
  
  override def receive = produce
  
  protected def process(event: Event) {
    // Not in use
  }

}
