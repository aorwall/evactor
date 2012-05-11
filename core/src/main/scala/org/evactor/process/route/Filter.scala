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
package org.evactor.process.route

import org.evactor.model.events.Event
import org.evactor.process.Processor
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription
import org.evactor.expression.Expression
import org.evactor.publish.Publisher
import com.typesafe.config.Config
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscriptions
import org.evactor.expression.Expression
import org.evactor.publish.Publication

/**
 * Filter out all events that do or don't match the specified expression
 */
class Filter (
    override val subscriptions: List[Subscription],
    val publication: Publication,
    val expression: Expression,
    val accept: Boolean)
  extends Processor(subscriptions) with Publisher {

  def process(event: Event) {
    
    expression.evaluate(event) match {
      case Some(true) if(accept) => publish(event)
      case Some(false) if(!accept) => publish(event)
      case _ => // do nothing
    }
    
  }

}