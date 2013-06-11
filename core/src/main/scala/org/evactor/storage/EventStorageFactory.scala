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

import com.typesafe.config.Config
import akka.actor.Extension
import akka.actor.ExtendedActorSystem
import akka.actor.ActorSystem
import scala.util.{Try, Success, Failure}

object EventStorageFactory {
  
  class Settings(val config: Config) {
    
    import config._

    val implPath = "evactor.storage.implementation"
    val StorageImplementation: Option[String] = if(hasPath(implPath)) {
      Some(getString("evactor.storage.implementation"))
    } else {
      None
    }

  }
}

class EventStorageFactory(val system: ExtendedActorSystem) extends Extension {
  
  import EventStorageFactory._
  
  val settings = new Settings(system.settings.config)	
	
  def getEventStorage(): Option[EventStorage] = storageImplementation
	
  def storageImplOf(storageFQN: String): Try[EventStorage] = 
    system.dynamicAccess.createInstanceFor[EventStorage](storageFQN, Seq(classOf[ActorSystem] -> system))
        
  lazy val storageImplementation: Option[EventStorage] = settings.StorageImplementation match {
    case Some(impl) => storageImplOf(impl) match {
      case Failure(e) => throw e
      case Success(v) => Some(v)
    }
    case None => None
  }
    

}
