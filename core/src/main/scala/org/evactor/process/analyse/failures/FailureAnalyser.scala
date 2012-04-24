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
package org.evactor.process.analyse.failures

import scala.collection.immutable.TreeMap
import org.evactor.model.attributes.HasState
import org.evactor.model.events.Event
import org.evactor._
import org.evactor.model.State
import org.evactor.process.analyse.window.Window
import org.evactor.process.analyse.Analyser
import akka.actor.ActorRef
import akka.actor.ActorLogging
import org.evactor.process.Subscription
import scala.collection.immutable.TreeSet
import org.evactor.model

class FailureAnalyser (
    override val subscriptions: List[Subscription], 
    override val channel: String, 
    override val category: Option[String],
    val maxOccurrences: Long)
  extends Analyser(subscriptions, channel, category) 
  with Window 
  with ActorLogging {

  type T = Event with HasState
  type S = State

  var allEvents = new TreeMap[Long, State]
  
  override def receive = {
    case event: Event with HasState => process(event) 
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.warning("{} is not an event with a state", msg)
  }

  override protected def process(event: T) {
    allEvents += (event.timestamp -> event.state)
      
    // Remove old
    val inactiveEvents = getInactive(allEvents)	
    allEvents = allEvents.drop(inactiveEvents.size)
	
    val failedEvents = allEvents.count { _._2 match {
      case model.Failure => true
      case _ => false
   	 }
    }
 
    log.debug("no of failed events from subscriptions {}: {}", subscriptions, failedEvents)

    if(failedEvents > maxOccurrences) {
      alert("%s failed events from subscriptions %s is more than allowed (%s)".format(failedEvents, subscriptions, maxOccurrences), Some(event))
    } else {
      backToNormal()
    }
  
  }
}
