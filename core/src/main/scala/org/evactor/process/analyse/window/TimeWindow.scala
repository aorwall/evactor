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
package org.evactor.process.analyse.window

import org.evactor.model.Timeout
import scala.concurrent.duration._
import akka.actor.ActorLogging
import scala.collection.immutable.SortedMap
import akka.actor.Cancellable

trait TimeWindow extends Window with ActorLogging {

  import context.dispatcher
  
  type S
  val timeframe: Long
    
  override protected[analyse] def getInactive(activities: SortedMap[Long, S]): Map[Long, S] = {
    
    // refresh scheduled timeout every time this method is executed
    schedule()
    
    activities.takeWhile( _._1 < System.currentTimeMillis - timeframe )
  }
  
  private def schedule() = {
    context.system.scheduler.scheduleOnce(timeframe milliseconds, self, Timeout)
  }

  abstract override def preStart = {
    log.debug("starting scheduler with timeframe set to {} ms", timeframe)
    schedule()
    super.preStart()
  }
  
  abstract override def postStop = {
    log.debug("stopping scheduler")
    super.postStop()
  }
}
