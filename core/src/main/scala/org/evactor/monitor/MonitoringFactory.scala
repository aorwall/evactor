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
package org.evactor.monitor

import com.typesafe.config.Config
import akka.actor.Extension
import akka.actor.ExtendedActorSystem
import akka.actor.ActorSystem
import scala.util.{Try, Success, Failure}

object MonitoringFactory {
  
  class Settings(val config: Config) {
    
    import config._

    val implPath = "evactor.monitoring.implementation"
    val MonitorImplementation: Option[String] = if(hasPath(implPath)) {
      Some(getString("evactor.monitoring.implementation"))
    } else {
      None
    }
  }
}

class MonitoringFactory(val system: ExtendedActorSystem) extends Extension {
  
  import MonitoringFactory._
  
  val settings = new Settings(system.settings.config) 
  
  def getMonitoring(): Option[Monitoring] = monitorImplementation
  
  def monitorImplOf(monitorFQN: String): Try[Monitoring] = 
    system.dynamicAccess.createInstanceFor[Monitoring](monitorFQN, Seq(classOf[ActorSystem] -> system))
        
  lazy val monitorImplementation: Option[Monitoring] = settings.MonitorImplementation match {
    case Some(impl) => monitorImplOf(impl) match {
      case Failure(e) => system.log.error("Failed to initiate monitoring, {}", e); None
      case Success(s) => Some(s)
    }
    case None => None
  }
}
