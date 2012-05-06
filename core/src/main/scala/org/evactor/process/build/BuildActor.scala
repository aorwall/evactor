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

import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._
import akka.util.duration._
import org.evactor.model.events.Event
import org.evactor.model.Timeout
import org.evactor.process._
import org.evactor.publish.Publication
import org.evactor.publish.Publisher

/**
 * One build actor for each running event builder
 */
abstract class BuildActor(
    val id: String, 
    val timeout: Long,
    val publication: Publication)
  extends EventBuilder 
  with Publisher 
  with ActorLogging {

  var scheduledFuture: Option[Cancellable] = None
  
  override def preStart() {
    log.debug("starting...")
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeout milliseconds, self, Timeout))
  }
  
  override def postRestart(reason: Throwable) {
  
    // TODO: in case of a restart, read active events from db (CF: activeEvent/name/id/****
    //storedEvents.foreach(event => eventBuilder.addEvent(event))
    
    if(isFinished){
      sendEvent(createEvent.fold(throw _, e => e))
    }
  }

  override def receive = {
    case event: Event => process(event)
    case Timeout => log.debug("%s: timeout!".format(id)); sendEvent(createEvent.fold(throw _, e => e))
    case msg => log.info("can't handle: {}", msg)
  }

  def process(event: Event) {

    //TODO: Save event to in activeEvents

    if(log.isDebugEnabled) log.debug("received event : %s".format(event) )

    addEvent(event)

    if(isFinished){
      log.debug("finished!")
      sendEvent(createEvent.fold(throw _, e => e))
    }
  }

  def sendEvent(event: Event) {

    // send the created event back to the processor event bus
    publish(event)
    
    stopScheduler()
    context.stop(context.self)
  }

  def eventExists(activityId: String) = false
  // TODO: Check if a finished event already exists
    
  override def postStop() {
    log.debug("stopping...")
    stopScheduler()
    clear()
  }
  
  private[this] def stopScheduler() = scheduledFuture match {
    case Some(s) => s.cancel()
    case None => 
  }

}

