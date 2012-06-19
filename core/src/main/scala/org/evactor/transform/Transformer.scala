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
package org.evactor.transform

import org.evactor.collect.Collector
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ReflectiveDynamicAccess
import com.typesafe.config.Config
import org.evactor.ConfigurationException
import scala.collection.JavaConversions._

/**
 * Transforms external events to the internal event representation
 */
trait Transformer extends Actor {

}


object Transformer {

  lazy val dynamicAccess = new ReflectiveDynamicAccess(this.getClass.getClassLoader)
  
  def apply(config: Config, sendTo: ActorRef): Transformer = {
    
    import config._
    
    if(hasPath("type")){
      getString("type") match{
        // No tranformer types implemented yet!
        case o => throw new ConfigurationException("tranformer type not recognized: %s".format(o))
      }
    } else if (hasPath("class")) {
      val clazz = getString("class")
      
      val args = if(hasPath("arguments")){
        getList("arguments").map { a => (a.unwrapped.getClass, a.unwrapped.asInstanceOf[AnyRef]) }
      } else {
        Nil
      }
            
      dynamicAccess.createInstanceFor[Transformer](clazz, Seq((classOf[ActorRef], sendTo)) ++ args).fold(throw _, p => p)
    } else {
      throw new ConfigurationException("transformer must specify either a type or a class")
    }
  }
}