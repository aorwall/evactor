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
package org.evactor.process.build.simpleprocess

import org.evactor.model.events._
import org.evactor.model._
import org.evactor.process._
import org.evactor.process.build._
import akka.actor.ActorRef
import org.evactor.process.ProcessorEventBus
import akka.actor.ActorLogging
import scala.collection._

/**
 * Processes simple processes with requests from subscribed channels from specified
 * components.
 */
class SimpleProcessBuilder(
    override val subscriptions: List[Subscription],
    val publication: Publication,
    val _components: List[String],
    val _timeout: Long) 
  extends Builder(subscriptions) 
  with Subscriber 
  with ActorLogging {

  override type T = RequestEvent

  def getEventId(logevent: RequestEvent) = logevent.correlationId

  def createBuildActor(id: String): BuildActor = 
    new BuildActor(id, _timeout, publication) 
      with SimpleProcessEventBuilder {
   	  val components = _components
   	}

}

trait SimpleProcessEventBuilder extends EventBuilder with ActorLogging {
    
  val components: List[String]
  
  var requests: List[RequestEvent] = List()
  var processedComponents: Set[String] = Set()
  
  var failed = false
  
  def addEvent(event: Event) = event match {
    case reqEvent: RequestEvent => addRequestEvent(reqEvent)  
    case _ =>    
  }

  def addRequestEvent(event: RequestEvent) {
    if(!processedComponents.contains(event.component) && components.contains(event.component)){
      requests ::= event
      processedComponents += event.component
    }
  }
    
  def isFinished(): Boolean = {
    if(requests.size == components.size) true
    else if ( requests.exists(isFailure(_)) ) true
    else false    
  }
  
  private[this] def isFailure(event: RequestEvent): Boolean = {
     event.state match {
         case Timeout => true
         case Cancellation => true
         case Failure => true
         case _ => false
       }  
  }
  
  private[this] def getCauseOfFailure(event: RequestEvent): State = {
    event.state match  {
         case Cancellation => Cancellation
         case Failure => Failure
         case _ => Timeout
    } 
  }
  

  def createEvent(): Either[Throwable, SimpleProcessEvent] = {
    
    val sortedRequests = requests.sortWith((e1, e2) => e1.timestamp < e2.timestamp)

    if (requests.size == components.size){
      Right(new SimpleProcessEvent(sortedRequests.last.id, sortedRequests.last.timestamp, sortedRequests.map(_.id), sortedRequests.last.state, sortedRequests.last.timestamp - sortedRequests.head.timestamp + sortedRequests.head.latency ))
    } else if (requests.size > 0){
      Right(new SimpleProcessEvent(sortedRequests.last.id, sortedRequests.last.timestamp, sortedRequests.map(_.id), getCauseOfFailure(sortedRequests.last), sortedRequests.last.timestamp - sortedRequests.head.timestamp + sortedRequests.head.latency ))
    } else {
      Left(new EventCreationException("SimpleProcessEventBuilder was trying to create an event with no request events"))
    }
  }

  protected def getState(reqEvent: RequestEvent) = reqEvent.state match {
     case Cancellation => Cancellation
     case Failure => Failure
     case _ => Timeout
  }
  
  def clear() {    
    requests = List[RequestEvent]()
    processedComponents = Set()
  }
}
