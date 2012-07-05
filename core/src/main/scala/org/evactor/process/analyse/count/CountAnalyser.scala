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
import org.evactor.process._
import org.evactor.model.events.Event
import org.evactor.process.analyse.window.TimeWindow
import akka.actor.ActorLogging
import akka.util.duration._
import org.evactor.publish.Publisher
import org.evactor.model.events.AlertEvent
import scala.collection.immutable.TreeMap
import java.util.UUID
import org.evactor.model.Timeout
import org.evactor.model.events.ValueEvent
import scala.None

/**
 * Counting events in channels and creates
 * a value event with current count
 * 
 */
class CountAnalyser (
    override val subscriptions: List[Subscription],
    val publication: Publication,
    override val categorization: Categorization, 
    val timeframe: Long) extends CategorizedProcessor(subscriptions, categorization) {

  protected def createCategoryProcessor(categories: Set[String]): CategoryProcessor = {
    new CountSubAnalyser(publication, categories, timeframe)
  }
}

class CountSubAnalyser (
    val publication: Publication,
    override val categories: Set[String],
    val timeframe: Long) 
  extends CategoryProcessor(categories)
  with TimeWindow 
  with Publisher
  with ActorLogging {
  
  type S = Long
  
  var allEvents = new TreeMap[Long, Long]
  var sum = 0L
  
  override def preStart = {
    log.debug("Starting sub counter with categories {} and timeframe {} ms", categories, timeframe)
    super.preStart()
  }
  
  override def postStop = {
    log.debug("Stopping sub counter with categories {} and timeframe {} ms", categories, timeframe)
    super.postStop()
  }
  
  override protected def process(event: Event) {
    analyse(Some(event))
  }
  
  protected def analyse(event: Option[Event]) {
    
    // Count up one if an event is provided
    var diff = event match {
      case Some(e) => {
        val count = allEvents.getOrElse(e.timestamp, 0L)+1
        allEvents += (e.timestamp -> count)
        1L
      }
      case None => 0L
    }
    
    // Remove old
    val inactiveEvents = getInactive(allEvents)
    if(inactiveEvents.size > 0){
      allEvents = allEvents.drop(inactiveEvents.size)
  
      log.debug("inactive events: {}", inactiveEvents)
      
      diff += inactiveEvents.foldLeft(0L) {
        case (a, (k, v)) => a - v
      }
    }
  
    // I the count changed, change sum and publish a value event
    if(diff != 0L || sum == 0){
      sum += diff;
      log.debug("there are currently {} active events ({})", sum, allEvents)
      
      val time = if(!allEvents.isEmpty){
        allEvents.last._1
      } else {
        currentTime
      }
      publish(new ValueEvent(uuid, time, categories, sum))
    }
    
    if(sum == 0){
      context.stop(context.self)
    }
  }
  
  override protected def timeout() {
    analyse(None)
  }
  
}