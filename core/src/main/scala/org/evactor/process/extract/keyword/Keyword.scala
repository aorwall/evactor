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
package org.evactor.process.extract.keyword

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import akka.actor.Actor
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import org.evactor.model.events.EventRef
import org.evactor.process.extract.Extractor
import org.evactor.process.ProcessorConfiguration
import org.evactor.process.Processor
import org.evactor.expression.MvelExpressionEvaluator
import org.evactor.expression.Expression
import org.evactor.expression.MvelExpression
import org.evactor.expression.XPathExpression
import org.evactor.expression.XPathExpressionEvaluator
import org.evactor.process.Subscription
import org.evactor.expression.ExpressionEvaluator
import org.evactor.process.extract.EventCreator

/**
 * Extracts a expression from a message and creates a new event object of the same type
 * with that expression as category.
 * 
 * Uses MVEL to evaluate expressions, will be extended later...
 */
class Keyword (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val expression: Expression) 
  extends ProcessorConfiguration(name, subscriptions) {

  override def processor: Processor = expression match {
	    case MvelExpression(expr) => new KeywordExtractor(subscriptions, channel, expr) 
	      with MvelExpressionEvaluator
	    case XPathExpression(expr) => new KeywordExtractor(subscriptions, channel, expr) 
	      with XPathExpressionEvaluator
	    case other => 
	      throw new IllegalArgumentException("Not a valid expression: " + other)
  }
}

class KeywordExtractor(
    override val subscriptions: List[Subscription], 
    override val channel: String,
    override val expression: String)
  extends Extractor (subscriptions, channel, expression) with EventCloner {
  
}

trait EventCloner extends EventCreator {
  
  def createBean(value: Option[Any], event: Event, newChannel: String): Option[Event] = value match {
    case Some(keyword: String) => 
    		   Some(event.clone(newChannel, Some(keyword)))
    case _ => None
  }
  
}
