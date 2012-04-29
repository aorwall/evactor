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
package org.evactor.process.build.request

import org.evactor.model.events.Event
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.Failure
import org.evactor.model.Start
import org.evactor.model.Success
import org.evactor.process.build._
import org.evactor.process.{Processor, Subscription, Publication}
import akka.actor.ActorLogging
import akka.actor.ActorRef
import org.evactor.model.Timeout
import java.util.UUID

/**
 * Handles LogEvent objects and creates a RequestEvent object. 
 */
class RequestBuilder (
    override val subscriptions: List[Subscription],
    val publication: Publication,
    val timeout: Long) 
  extends Builder(subscriptions) 
  with ActorLogging {
  
  type T = LogEvent
       
  /*
   * Accepts all componentId:s
   */
  def handlesEvent(event: LogEvent) = true

  def getEventId(logevent: LogEvent) = logevent.correlationId

  def createBuildActor(id: String): BuildActor = {
    new BuildActor(id, timeout, publication) with RequestEventBuilder
  }
  
}

trait RequestEventBuilder extends EventBuilder with ActorLogging {
  
  var startEvent: Option[LogEvent] = None
  var endEvent: Option[LogEvent] = None

  def addEvent(event: Event) = event match {
    case logevent: LogEvent => addLogEvent(logevent)  
    case _ =>    
  }

  def addLogEvent(logevent: LogEvent) {
	 logevent.state match {
	   case Start => startEvent = Some(logevent)
	   case Success => endEvent = Some(logevent)
	   case Failure => endEvent = Some(logevent)
	   case state => log.warning("Unknown state on log event: {}", state)
	 }
  }

  def isFinished(): Boolean = startEvent != None && endEvent != None

  def createEvent(): Either[Throwable, RequestEvent] = (startEvent, endEvent) match {
    case (Some(start: LogEvent), Some(end: LogEvent)) =>
      Right(new RequestEvent(UUID.randomUUID.toString, end.timestamp, end.correlationId, end.component, Some(start.id), Some(end.id), end.state, end.timestamp - start.timestamp ))
    case (Some(start: LogEvent), _) =>
      Right(new RequestEvent(UUID.randomUUID.toString, System.currentTimeMillis, start.correlationId, start.component, Some(start.id), None, Timeout, 0L))
    case (_, end: LogEvent) =>
      Left(new EventCreationException("RequestProcessor was trying to create an event with only an end log event. End event: " + end))
    case (_, _) =>
      Left(new EventCreationException("RequestProcessor was trying to create an event without either a start or an end log event."))
  }

  def clear() {
    startEvent = None
    endEvent = None
  }

}
