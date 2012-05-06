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
package org.evactor.process.analyse.latency

import collection.immutable.TreeMap
import akka.actor.ActorRef
import org.evactor.model.events.Event
import org.evactor.model.attributes.HasLatency
import org.evactor.process.analyse.Analyser
import org.evactor.process.analyse.window.Window
import akka.actor.ActorLogging
import org.evactor.subscribe.Subscription
import org.evactor.publish.Publication

class LatencyAnalyser(
    override val subscriptions: List[Subscription], 
    override val publication: Publication,
    val maxLatency: Long)
  extends Analyser(subscriptions, publication) with Window with ActorLogging {

  type T = Event with HasLatency
  type S = Long

  var events = new TreeMap[Long, Long]()
  var sum = 0L
  
  override protected def process(e: Event)  = e match {
    case event: Event with HasLatency => {
	
      log.debug("received: {}", event)

      // Add new
      val latency = event.latency
      events += (event.timestamp -> latency)
      sum += latency

      // Remove old
      val inactiveEvents = getInactive(events)
    
      events = events.drop(inactiveEvents.size)   
    
      sum += inactiveEvents.foldLeft(0L) {
    	case (a, (k, v)) => a - v
      }
    
      // Count average latency
      val avgLatency = if (sum > 0) {
        sum / events.size
      } else {
        0
      }

      log.debug("sum: {}, no of events: {}, avgLatency: {}", sum, events.size, avgLatency)

      if (avgLatency > maxLatency) {
        alert("Average latency %s ms is higher than the maximum allowed latency %s ms".format(avgLatency, maxLatency), Some(event))
      } else {
        backToNormal()
      }
    }
    case msg => log.warning("event must extend HasLatency: {}", msg)   
  }
}
