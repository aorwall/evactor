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
package org.evactor.process.extract

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import org.evactor.process._
import org.evactor.expression.Expression
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription
import org.evactor.publish.Publisher

/**
 * Extract information from messages and creates new events based on the extracted
 * value.
 * 
 * It will first evaluate the message in the event with an ExpressionEvaluator
 * and then create a new Event, based on the evaluated message, with an EventCreator.
 */
abstract class Extractor(
    override val subscriptions: List[Subscription], 
    val publication: Publication,
    val expression: Expression) 
  extends Processor(subscriptions) 
  with EventCreator
  with Publisher
  with ActorLogging {
  
  override protected def process(event: Event) = event match {
    case e: HasMessage => {
      log.debug("will extract values from {}", e )
    
      createBean(expression.evaluate(e), e) match {
        case Some(event) => publish(event)
        case None => log.info("couldn't extract anything from event: {}", e)
      }
    }
    case msg => log.warning("{} is of wrong type", msg)
  }
}
