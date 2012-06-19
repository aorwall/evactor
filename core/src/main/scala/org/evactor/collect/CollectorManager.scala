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
package org.evactor.collect

import com.typesafe.config.Config
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.duration._
import scala.collection.JavaConversions._
import org.evactor.ConfigurationException
import org.evactor.Start
import akka.camel.CamelExtension
import org.apache.activemq.camel.component.ActiveMQComponent

class CollectorManager extends Actor with ActorLogging {

  val config = context.system.settings.config

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e: ConfigurationException => log.error("Stopping collector because of: {}", e); Stop
    case e: ClassNotFoundException => log.error("Stopping collector because of: {}", e); Stop
    case e: ActorInitializationException => log.error("Stopping collector because of: {}", e); Stop
    case e: Exception => log.error("Caught exception: {}", e); Restart
  }

  def receive = {
    case (name: String, config: Config) => addCollector(name, config)
    case name: String => removeCollector(name)
    case Start => start()
    case msg => log.debug("can't handle {}", msg)
  }

  def start() {
    val camel = CamelExtension(context.system)

    //Add camel brokers
    if(config.hasPath("evactor.brokers")){
      config.getConfig("evactor.brokers").root.keySet.foreach { broker =>
        val brokerUri = config.getConfig("evactor.brokers").getString(broker)
        camel.context.addComponent(broker, ActiveMQComponent.activeMQComponent(brokerUri))
      }
    }

    config.getConfig("evactor.collectors").root.keySet.foreach { k =>
      addCollector(k, config.getConfig("evactor.collectors").getConfig(k))
    }
    
  }
  
  private[this] def addCollector(n: String, c: Config) {
    try {
      log.debug("starting collector with configuration: {}", c)
      
      context.actorOf(Props(Collector(c)), name = n)
      sender ! Status.Success
    } catch {
      case e: Exception => {
        log.warning("Starting collector with name {} failed. {}", n, e)
        sender ! Status.Failure(e)
        throw e
      }
    }
  }
  
  private[this] def removeCollector(name: String) {
    // TODO
  }
}

