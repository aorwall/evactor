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
package org.evactor.process.alert

import akka.actor.ActorLogging
import org.evactor.subscribe.Subscription
import org.evactor.process._
import org.evactor.publish.Publication
import org.evactor.publish.Publisher
import org.evactor.expression.Expression
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.Event
import java.util.UUID
import org.evactor.EvactorException

/**
 * Alerts when the specified expression isn't followed (ie returns false).
 * 
 * An alert will be sent the first time the expression isn't followed. 
 * And a back to normal-alert when the opposite occurs.
 */
class Alerter(
    override val subscriptions: List[Subscription], 
    val publication: Publication,
    override val categorization: Categorization, 
    val expression: Expression) 
  extends CategorizedProcessor(subscriptions, categorization) 
  with Publisher
  with ActorLogging {
  
  protected def createCategoryProcessor(categories: Set[String]): CategoryProcessor = {
    new SubAlerter(publication, categories, expression)
  }
}

class SubAlerter(
    val publication: Publication,
    override val categories: Set[String],
    val expression: Expression) 
  extends CategoryProcessor(categories) 
  with Publisher
  with ActorLogging {
  
  import context._
  
  override def preStart {
    // TODO: Initiate from persisted state!
    become(untriggered)
    super.preStart()
  }
  
  def triggered: Receive = {
    case e: Event if(!evaluate(e)) => become(untriggered); backToNormal(e)
    case _ => // Do nothing
  }
  
  def untriggered: Receive = {
    case e: Event if(evaluate(e)) => become(triggered); alert(e)
    case _ => // Do nothing
  }
  
  def process(event: Event) {
    // Not in use
  }
  
  protected def alert(event: Event) {
    log.debug("Alert: {}", event)
    publish(new AlertEvent(uuid, currentTime, categories, true, event))
  }

  protected def backToNormal(event: Event) {
    log.debug("Back to normal: {}", event)
    publish(new AlertEvent(uuid, currentTime, categories, false, event))
  }
  
  protected def evaluate(event: Event): Boolean = {
    val answer = expression.evaluate(event)
    answer match {
      case Some(b: Boolean) => b
      case _ => throw new ProcessException("No boolean value could be extracted from %s with %s".format(event, expression))
    }
  }
  
}