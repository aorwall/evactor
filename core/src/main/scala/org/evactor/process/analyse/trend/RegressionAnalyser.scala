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
package org.evactor.process.analyse.trend

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
import scala.collection.mutable.LinkedList
import java.util.ArrayList
import org.evactor.monitor.Monitored
import org.evactor.model.events.ValueEvent

/**
 * Analysing trends by checking the regression coefficient on growth of event occurrences within
 * a specified timeframe
 * 
 * I'm not really sure how correct this is...
 * 
 */
class RegressionAnalyser (
    override val subscriptions: List[Subscription],
    val publication: Publication,
    override val categorization: Categorization,
    val minSize: Long, 
    val timeframe: Long) extends CategorizedProcessor(subscriptions, categorization) {

  protected def createCategoryProcessor(categories: Set[String]): CategoryProcessor =
    new RegressionSubAnalyser(publication, categories, minSize, timeframe)
}

class RegressionSubAnalyser (
    val publication: Publication,
    override val categories: Set[String],
    val minSize: Long, 
    val timeframe: Long) 
  extends CategoryProcessor(categories)
  with TimeWindow 
  with Publisher
  with Monitored
  with ActorLogging {
  
  type S = Event
  
  val start = System.currentTimeMillis
  protected[trend] val eventCount = new Array[Long](10)
  var iteration = start
  var currentCoeff = 0.0
  
  override def preStart = {
    log.debug("Starting sub counter with categories {} and timeframe {} ms", categories, timeframe)
    super.preStart()
  }
  
  override def postStop = {
    log.debug("Stopping sub counter with categories {} and timeframe {} ms", categories, timeframe)
    removeLabel()
    super.postStop()
  }
  
  override protected def process(event: Event) {
    
    val now = System.currentTimeMillis
    
    moveCounters(now)
    
    // count new event
    val i = ((now - event.timestamp) / (timeframe/10)).toInt
    if(i < 10 && i >= 0){
      eventCount.update(i, eventCount(i)+1)
    }
    
    if(eventCount.sum > minSize && now > start + timeframe/2){
      
      val listToCheck = eventCount.toList.drop(1).reverse.dropWhile(_ == 0)
      if(listToCheck.size > 6){
         
        val coeff = leastSquares(listToCheck)
      
//        addLabel("coefficient: %s (%s), id: %s, eventCount: %s".format(coeff, coefficient, id, listToCheck))
        
//          log.info("coefficient: {}, id: {}, eventCount: {}", coefficient, id, listToCheck)
        if(currentCoeff != coeff){
          publish(new ValueEvent(uuid, currentTime, categories, coeff))
          currentCoeff = coeff;
        }

      }
    }
  }
  
  def moveCounters(now: Long) {
    
    if(now > iteration + timeframe/10){
      val iterations = ((now - iteration) / (timeframe / 10)).toInt
      
      for(i <- iterations-1 until 10){
        if(9-i+iterations < 10){
          eventCount.update(9-i+iterations, eventCount(9-i))
        } 
      }
      
      val max = if(iterations < 10) iterations else 10
      for(i <- 0 until max){
        eventCount.update(i, 0)
      }
      iteration = iteration + (timeframe / 10) * (iterations + 1) 
    }
    
  }
  
  def leastSquares(ys: List[Long]): Double = {
    val xs =  1 until ys.size+1
    val totalSum = xs.zip( ys ).map{ case (x, y) => x * y}.sum
    val numerator = totalSum - (xs.sum * ys.sum) / xs.size 
    val denominator = xs.map(x => x*x).sum - (xs.sum * xs.sum) / xs.size
    numerator.toDouble / denominator.toDouble
  }
  
  private[this] def stop() {
    cancellable.cancel()
    context.stop(context.self)
  }
  
  override protected def timeout() {
    moveCounters(System.currentTimeMillis)
    if(eventCount.sum == 0){
      stop()
    }
  }
  
}