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
package org.evactor.process.analyse.absence

import akka.actor.{Cancellable}
import akka.util.duration._
import org.evactor.model.events.Event
import org.evactor.process.analyse.Analyser
import org.evactor.model.Timeout
import akka.actor.ActorRef
import akka.actor.ActorLogging
import org.evactor.model.Message
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription

/**
 * TODO: Check event.timestamp to be really sure about the timeframe between events 
 */
class AbsenceOfRequestsAnalyser (
    override val subscriptions: List[Subscription], 
    override val publication: Publication, 
    val timeframe: Long)
  extends Analyser(subscriptions, publication) 
  with ActorLogging {
  
  var scheduledFuture: Option[Cancellable] = None
    
  override protected def process(event: Event) {
    log.debug("received: " + event)

    // TODO: Check event.timestamp to be really sure about the timeframe between events
    backToNormal()
    
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => log.warning("no scheduler set in Absence of request analyser with subscriptions: {}", subscriptions)
    }
    startScheduler()
  }

  override protected def timeout() = {
    alert("No events within the timeframe %s ms".format(timeframe))
  }
  
  def startScheduler() {
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, Timeout))
  }
  
  override def preStart() {
    log.debug("Starting with timeframe: {}", timeframe)
    startScheduler()
  }

  override def postStop() {
    log.debug("Stopping...")
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => log.warning("No scheduler set in Absence of request analyse with subscriptions: {}", subscriptions)
    }
  }

}
