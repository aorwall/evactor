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
package org.evactor.process.analyse.latency

import org.evactor.process.ProcessorConfiguration
import org.evactor.process.analyse.window.TimeWindow
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.process.analyse.window.WindowConf
import org.evactor.process.analyse.window.TimeWindowConf
import org.evactor.process.analyse.window.LengthWindowConf
import org.evactor.process.Subscription
import org.evactor.utils.JavaHelpers.any2option
import org.evactor.process.Publication

class Latency (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val publication: Publication,
    val maxLatency: Long, 
    val window: Option[WindowConf])
  extends ProcessorConfiguration(name, subscriptions) {

  def this(name: String, subscription: Subscription, publication: Publication,
    maxLatency: Long, window: WindowConf) = {
    this(name, List(subscription), publication, maxLatency, window)
  }
  
  def processor = window match {
    case Some(length: LengthWindowConf) => 
      new LatencyAnalyser(subscriptions, publication, maxLatency) 
        with LengthWindow { override val noOfRequests = length.noOfRequests }
    case Some(time: TimeWindowConf) => 
      new LatencyAnalyser(subscriptions, publication, maxLatency)
        with TimeWindow { override val timeframe = time.timeframe }
    case None => new LatencyAnalyser(subscriptions, publication, maxLatency)
  }
}
