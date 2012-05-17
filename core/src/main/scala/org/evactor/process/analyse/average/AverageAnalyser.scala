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
package org.evactor.process.analyse.average

import collection.immutable.TreeMap
import akka.actor.ActorRef
import org.evactor.model.events.Event
import org.evactor.model.attributes.HasLatency
import org.evactor.process.analyse.window.Window
import akka.actor.ActorLogging
import org.evactor.subscribe.Subscription
import org.evactor.publish.Publication
import org.evactor.process.Processor
import org.evactor.expression.Expression
import org.evactor.process.CategoryProcessor
import org.evactor.process.SubProcessor
import org.evactor.publish.Publisher
import org.evactor.model.events.ValueEvent
import java.util.UUID
import org.evactor.EvactorException
import com.typesafe.config.Config
import org.evactor.process.analyse.window.TimeWindow
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.ConfigurationException

class AverageAnalyser(
    override val subscriptions: List[Subscription], 
    val publication: Publication,
    override val categorize: Boolean,
    val expression: Expression,
    val windowConf: Option[Config])
  extends CategoryProcessor(subscriptions, categorize) with ActorLogging {

  // TODO: Closure  
  protected def createSubProcessor(id: String): SubProcessor = windowConf match {
    case Some(c) => {
      if(c.hasPath("time")){
        new AverageSubAnalyser(publication, id, expression) with TimeWindow { override val timeframe = c.getMilliseconds("time").toLong }
      } else if(c.hasPath("length")) {
        new AverageSubAnalyser(publication, id, expression) with LengthWindow { override val noOfRequests = c.getInt("length") }
      } else {
        throw new ConfigurationException("window configuration not recognized: %s".format(c))
      }
    }
    case None =>  new AverageSubAnalyser(publication, id, expression)
  }
}
 
class AverageSubAnalyser(
    val publication: Publication,
    override val id: String,
    val expression: Expression)
  extends SubProcessor(id) with Publisher with Window with ActorLogging {
 
  type S = Double

  var events = new TreeMap[Long, Double]()
  var sum = 0.0
  
  override protected def process(event: Event) = {
    
    log.debug("received: {}", event)

    val value = expression.evaluate(event) match {
      case Some(v: Number) => v.doubleValue
      case Some(v: Double) => v
      case Some(v: Int) => v.doubleValue
      case _ => throw new EvactorException("No numeric value could be extracted from %s with %s".format(event, expression))
    }
    
    events += (event.timestamp -> value)
    sum += value
    
    analyse()
  }
  
  protected def analyse() {
    
    // Remove old
    val inactiveEvents = getInactive(events)
    
    events = events.drop(inactiveEvents.size)   
    
    sum += inactiveEvents.foldLeft(0.0) {
      case (a, (k, v)) => a - v
    }
    
    log.debug("sum: {}, no of events: {}", sum, events.size)
    
    // Count average
    if (events.size > 0) {
      sendValue(sum / events.size)
    } else {
      sendValue(0)
      context.stop(context.self)
    }
  }
  
  protected def sendValue(value: Any){    
    publish(new ValueEvent(uuid, currentTime, value))
  }
  
  override protected def timeout() {
    analyse()
  }
  
}
