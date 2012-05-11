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
package org.evactor

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import org.evactor.collect.CollectorManager
import org.evactor.process.ProcessorManager
import org.evactor.storage.StorageManager
import akka.actor.OneForOneStrategy
import akka.util.duration._
import akka.actor.SupervisorStrategy._

class EvactorContext extends Actor with ActorLogging {

  import context._
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: IllegalArgumentException => Stop
    case _: Exception => Resume // We don't want to restart the managers ....
  }
  
  // managers
  lazy val pm = actorOf(Props[ProcessorManager], name = "process")
  lazy val cm = actorOf(Props[CollectorManager], name = "collect")
  lazy val sm = actorOf(Props[StorageManager], name = "store")
  
  lazy val storageImpl = system.settings.config.getString("evactor.storage.implementation")
  
  def receive = {
    case _ =>
  }
  
  override def preStart() = {
    log.info("Starting Evactor")
    
    pm ! Start
    cm ! Start
    if(system.settings.config.hasPath("evactor.storage.implementation")) sm ! Start
  }
  
  override def postStop = {
    log.info("Stopping Evactor")
  }
}

case class Start