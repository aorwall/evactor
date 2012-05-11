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
package org.evactor.process

import scala.collection.JavaConversions._
import org.evactor.model.events.Event
import org.evactor.storage.StorageProcessor
import org.evactor.storage.StorageProcessorRouter
import com.typesafe.config.Config
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.duration._
import org.evactor.ConfigurationException
import org.evactor.Start

/**
 * Handles all processors.
 */
class ProcessorManager extends Actor with ActorLogging  {
    
  val config = context.system.settings.config
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: ConfigurationException => Stop
    case _: Exception => Restart
  }

  def receive = {
    case (name: String, configuration: Config) => setProcessor(name, configuration)
    case name: String => removeProcessor(name)
    case Start => start()
    case msg => log.warning("can't handle: {}", msg); sender ! Status.Failure
  }
  
  def start() = {
    log.debug("loading processors from configuration")
    
    config.getConfig("evactor.processors").root.keySet.foreach { k =>
      setProcessor(k, config.getConfig("evactor.processors").getConfig(k))
    }
    
  }
  
  /**
   * Add and start new processor in the actor context. Will fail if
   * an exception is thrown on startup.
   */
  def setProcessor(name: String, configuration: Config) {
    try {
	    log.debug("starting processor for configuration: {}", config)
	    
      context.actorOf(Props(Processor(configuration)), name = name)
      sender ! Status.Success
    } catch {
			case e: Exception => {
			  log.warning("Starting processor with name {} failed. {}", configuration.getString("name"), e)
			  sender ! Status.Failure(e)
			}
	  }
  }

  def removeProcessor(name: String) {    
    try {
	    log.debug("stopping processor with name: {}", name)
	    val runningActor = context.actorFor(name)
	    context.stop(runningActor)  
	    sender ! Status.Success    
    } catch {
			case e: Exception => sender ! Status.Failure(e)
	  }
  }

  override def postStop() {
    log.debug("stopping...")
  }
}

