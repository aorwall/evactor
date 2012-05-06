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
package org.evactor.process.analyse

import akka.actor.{Actor, ActorRef, ActorLogging}
import scala.Predef._
import org.evactor.model.events.{Event, AlertEvent}
import org.evactor.process._
import java.util.UUID
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription
import org.evactor.publish.Publisher

/**
 * An analyser analyses event flows and creates alert events when 
 * triggered. It will only create an alert the first time it's 
 * triggered and then wait until state is back to normal again.
 * 
 * TODO: Should do some kind of check on timestamp on events if
 * events arrive in the wrong order.
 * 
 */
abstract class Analyser(
    override val subscriptions: List[Subscription], 
    val publication: Publication) 
  extends Processor(subscriptions) 
  with Publisher
  with ActorLogging {

  var triggered = false 
  
  protected def alert(message: String, event: Option[Event] = None) {
    if (!triggered) {
      log.debug("Alert: {}", message)
      triggered = true
      sendAlert( message, event)
    }
  }

  protected def backToNormal(event: Option[Event] = None) {
    if (triggered) {
      log.debug("Back to normal")

      triggered = false
      sendAlert("Back to normal", event)
    }
  }

  def sendAlert(message: String, event: Option[Event]) {
    val currentTime = System.currentTimeMillis
    
    val eventRef = event match {
      case Some(e) => Some(e.id)
      case None => None
    }
    
    val uuid = UUID.randomUUID.toString
    
    val alert = 
      new AlertEvent(
        uuid,
        currentTime, 
        triggered, 
        message,
        eventRef)
    
    publish(alert)

  }
  
}
