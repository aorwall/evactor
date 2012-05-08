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
package org.evactor.storage

import scala.util.DynamicVariable
import com.typesafe.config.Config
import akka.actor.Extension
import akka.config.ConfigurationException
import org.evactor.model.events.Event
import akka.actor.ExtendedActorSystem
import akka.actor.ActorSystem
import java.util.logging.Logger

object EventStorageFactory {
  
  class Settings(val config: Config) {
    
    import config._

    val implPath = "akka.evactor.storage.implementation"
    val StorageImplementation: Option[String] = if(hasPath(implPath)) {
      Some(getString("akka.evactor.storage.implementation"))
    } else {
      None
    }

  }
}

class EventStorageFactory(val system: ExtendedActorSystem) extends Extension {
  
  import EventStorageFactory._
  
  val settings = new Settings(system.settings.config)	
	
  def getEventStorage(): Option[EventStorage] = storageImplementation
	
  def storageImplOf(storageFQN: String): Either[Throwable, EventStorage] = 
    system.dynamicAccess.createInstanceFor[EventStorage](storageFQN, Seq(classOf[ActorSystem] -> system))
        
  lazy val storageImplementation: Option[EventStorage] = settings.StorageImplementation match {
    case Some(impl) => storageImplOf(impl).fold(throw _, Some(_)) 
    case None => None
  }
    

}
