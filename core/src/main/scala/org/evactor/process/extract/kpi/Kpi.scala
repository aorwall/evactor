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
package org.evactor.process.extract.kpi
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import org.evactor.model.events.KpiEvent
import org.evactor.process.extract.Extractor
import org.evactor.process.Processor
import org.evactor.process.ProcessorConfiguration
import org.evactor.expression.MvelExpressionEvaluator
import org.evactor.process.Subscription
import org.evactor.process.extract.EventCreator
import org.evactor.expression.MvelExpression
import org.evactor.expression.XPathExpressionEvaluator
import org.evactor.expression.XPathExpression
import org.evactor.expression.Expression
import akka.actor.ActorLogging
import org.evactor.process.Publisher
import org.evactor.process.Publication

/**
 * Extracts a value from a message and creates a KPI Event object. Using a specified
 * channel and the same category as the provided event if one exists
 * 
 * Uses MVEL to evaluate expressions and must return a float value, will be extended later...
 */
class Kpi (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val publication: Publication,
    val expression: Expression) 
  extends ProcessorConfiguration(name, subscriptions) {
   
  override def processor: Processor = expression match {
	    case MvelExpression(expr) => new KpiExtractor(subscriptions, publication, expr) 
	    	with MvelExpressionEvaluator
	    case XPathExpression(expr) => new KpiExtractor(subscriptions, publication, expr) 
	    	with XPathExpressionEvaluator
	    case other => 
	      throw new IllegalArgumentException("Not a valid expression: " + other)
  }
}

class KpiExtractor(
    override val subscriptions: List[Subscription], 
    override val publication: Publication,
    override val expression: String)
  extends Extractor (subscriptions, publication, expression) 
  with KpiEventCreator 
  with Publisher {
  
}

trait KpiEventCreator extends EventCreator {
  
  def createBean(value: Option[Any], event: Event with HasMessage): Option[Event] = value match {
    case Some(value: String) => try {
      Some(new KpiEvent(event.id, event.timestamp, value.toDouble)) 
    } catch {
      case _ => None
    }
    case a => None
  }
    
}
