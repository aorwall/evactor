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
package org.evactor.process

import akka.actor.ActorRef
import org.evactor.model.events.Event
import org.evactor.expression.Expression
import org.evactor.model.attributes.HasMessage

/**
 * Specifies how the event should be published
 */
abstract class Publication {

  def channel(event: Event): String
  def category(event: Event): Option[String] 
  
}

/**
 * Publishing events to a specified channel and category
 */
case class StaticPublication (
    val channel: String,
    val category: Option[String]) extends Publication {
    
  def channel(event: Event) = channel
  def category(event: Event) = category
    
}

/**
 * Extracting values from events with the HasMessage trait
 * to decide where to publish the event.
 */
case class DynamicCategoryPublication (
    val channelExpr: Expression,
    val categoryExpr: Option[Expression]) extends Publication {
  
  def channel(event: Event) = extract(channelExpr, event).get
  
  def category(event: Event) = categoryExpr match {
    case Some(expr) => extract(expr, event)
    case None => None
  }
  
  private[this] def extract(expr: Expression, event: Event) = event match {
    case e: Event with HasMessage => expr.evaluate(e) match {
      case Some(v: String) => Some(v)
      case _ => throw new PublishException("couldn't extract a category from event %s with expression %s".format(e, expr))
    }
    case e => throw new PublishException("event %s must extend HasMessage".format(e))
  }
  
}

/**
 * Publishing events to a specified actor (used for testing)
 */
case class TestPublication (val testActor: ActorRef) extends Publication {
    
  def channel(event: Event) = "none"
  def category(event: Event) = None
    
}

