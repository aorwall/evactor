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
package org.evactor.publish

import org.evactor.model.events.Event
import org.evactor.expression.Expression
import scala.collection.immutable.TreeSet

/**
 * Extracting values from events with the HasMessage trait
 * to decide where to publish the event.
 */
case class DynamicPublication (
    val channelExpr: Expression,
    val categoryExpr: Option[Expression]) extends Publication {
  
  def channel(event: Event) = channelExpr.evaluate(event) match {
      case Some(v: Any) => v.toString
      case _ => throw new PublishException("couldn't extract a channel from event %s with expression %s".format(event, channelExpr))
  }
  
}
