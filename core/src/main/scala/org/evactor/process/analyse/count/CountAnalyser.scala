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
package org.evactor.process.analyse.count

import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription
import org.evactor.process.Processor
import org.evactor.process.CategoryProcessor
import org.evactor.process.SubProcessor
import org.evactor.model.events.Event
import org.evactor.process.analyse.window.TimeWindow
import akka.actor.ActorLogging
import akka.util.duration._
import org.evactor.publish.Publisher
import org.evactor.model.events.AlertEvent
import scala.collection.immutable.TreeMap
import java.util.UUID
import org.evactor.model.Timeout

/**
 * Counting events in channels and alerts if they
 * occur more often than specified.
 * 
 */
class CountAnalyser (
    override val subscriptions: List[Subscription],
    val publication: Publication,
    override val categorize: Boolean, 
    val maxOccurrences: Long,
    val timeframe: Long) extends CategoryProcessor(subscriptions, categorize) {

  protected def createSubProcessor(id: String): SubProcessor = {
    new CountSubAnalyser(publication, id, maxOccurrences, timeframe)
  }
}

class CountSubAnalyser (
    val publication: Publication,
    override val id: String,
    val maxOccurrences: Long,
    val timeframe: Long) 
  extends SubProcessor(id) 
  with TimeWindow 
  with Publisher
  with ActorLogging {
  
  type S = Event
  
  var allEvents = new TreeMap[Long, Event]
  
  lazy val cancellable = context.system.scheduler.schedule(timeframe milliseconds, timeframe milliseconds, context.self, Timeout)
  
  override def preStart = {
    log.debug("Starting sub counter with id {} and timeframe {} ms", id, timeframe)
    cancellable
    super.preStart()
  }
  
  override def postStop = {
    log.debug("Stopping sub counter with id {} and timeframe {} ms", id, timeframe)
    super.postStop()
  }
  
  override protected def process(event: Event) {
    
    allEvents += (event.timestamp -> event)
    
    // Remove old
    val inactiveEvents = getInactive(allEvents) 
    allEvents = allEvents.drop(inactiveEvents.size)

    log.debug("received {}, there are currently {} active events", event, allEvents.size)
        
    if(allEvents.size > maxOccurrences){
      
      val alert = new AlertEvent(
          UUID.randomUUID.toString, allEvents.last._2.timestamp, true, id, None)
      publish(alert)
      stop()
      
    }
    
  }
  
  private[this] def stop() {
    cancellable.cancel()
    context.stop(context.self)
  }
  
  override protected def timeout() {
    val inactiveEvents = getInactive(allEvents) 
    allEvents = allEvents.drop(inactiveEvents.size)
    
    if(allEvents.size == 0){
      stop()
    }
    
  }
  
}