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

/**
 * Handling event storage. Made in the same way as the serialization
 * implementations in Akka.
 *  
 */
object EventStorageFactory {
  
  class Settings(val config: Config) {

    import scala.collection.JavaConverters._
    import config._

    val StorageImplementations: Map[String, String] =
      getConfig("akka.evactor.storage.implementations").root.unwrapped.asScala.toMap.map { case (k, v) => (k, v.toString) }

    val StorageBindings: Map[String, Seq[String]] = {
      val configPath = "akka.evactor.storage.storage-bindings"
      hasPath(configPath) match {
        case false => Map()
        case true => getConfig(configPath).root.unwrapped.asScala.toMap.map {
            case (k: String, v: java.util.Collection[_]) => (k -> v.asScala.toSeq.asInstanceOf[Seq[String]])
            case invalid => throw new ConfigurationException("Invalid storage-bindings [%s]".format(invalid))
          }
      }
    }
  }
}

class EventStorageFactory(val system: ExtendedActorSystem) extends Extension {
  
  import EventStorageFactory._
  
  val settings = new Settings(system.settings.config)	
	
  def getEventStorage(event: Event): Option[EventStorage] = {
    storageImplMap.get(event.getClass().getName).getOrElse(None)
  }  

  def getEventStorage(className: String): Option[EventStorage] = {
    storageImplMap.get(className).getOrElse(None)
  }  
	
  def storageImplOf(storageFQN: String): Either[Throwable, EventStorage] = 
    system.dynamicAccess.createInstanceFor[EventStorage](storageFQN, Seq(classOf[ActorSystem] -> system))
        
  lazy val storageImplementations: Map[String, Option[EventStorage]] = {     
    val storageConf = settings.StorageImplementations
    
    for ((k: String, v: String) <- storageConf)
      yield k -> storageImplOf(v).fold(throw _, Some(_)) // TODO: we don't care about exceptions atm...
  }

  lazy val bindings: Map[String, String] = {
    settings.StorageBindings.foldLeft(Map[String, String]()) {
      // All keys which are lists, take the Strings from them and Map them
      case (result, (k: String, vs: Seq[_])) => 
        result ++ (vs collect { case v: String => (v, k) })
      // For any other values, just skip them
      case (result, _) => result
    }
  }

  lazy val storageImplMap: Map[String, Option[EventStorage]] = bindings mapValues storageImplementations
   
}
