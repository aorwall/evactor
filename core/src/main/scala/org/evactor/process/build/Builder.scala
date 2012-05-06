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
package org.evactor.process.build

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import org.evactor.model.events.Event
import org.evactor.process.Processor
import scala.collection.mutable.HashMap
import org.evactor.process.Subscription

abstract class Builder (override val subscriptions: List[Subscription]) 
  extends Processor (subscriptions) 
  with ActorLogging {
  
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 60 seconds) {
	 case e: EventCreationException => log.warning("stopping on exception!"); Stop
	 case e: Exception => Restart
  }

  override protected def process(event: Event) {
    if(handlesEvent(event)){
      
      if(log.isDebugEnabled) log.debug("about to process event: {} ", event)

      val eventId = getEventId(event)    
      if(log.isDebugEnabled) log.debug("looking for active event builder with id: {}", eventId)
      val actor = getBuildActor(eventId)
    
      actor ! event
    } else {
      log.warning("can't handle event: {}", event)
    }
  }

  def handlesEvent(event: Event): Boolean
  
  def getEventId(event: Event): String
  
  def createBuildActor(id: String): BuildActor
  
  def getBuildActor(eventId: String): ActorRef = 
    context.children.find(_.path.name == eventId).getOrElse(context.actorOf(Props(createBuildActor(eventId)), eventId))
  
}
