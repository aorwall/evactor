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
package org.evactor.publish

import org.evactor.expression.Expression
import org.evactor.model.events.Event
import com.typesafe.config.Config
import akka.actor.ActorRef
import org.evactor.ConfigurationException
import scala.collection.JavaConversions._
import scala.collection.immutable.TreeSet
import com.typesafe.config.ConfigValueType

object Publication {
  
  def apply(config: Config): Publication = {
    
    val chanType = config.getValue("channel").valueType
    val _channel = if(chanType == ConfigValueType.STRING){
      val str = config.getString("channel")
      (event: Event) => str
    } else if (chanType == ConfigValueType.OBJECT){
      val expr = Expression(config.getConfig("channel"))
      (event: Event) => { expr.evaluate(event) match {
          case Some(v: Any) => v.toString
          case _ => throw new PublishException("couldn't extract a channel from event %s with expression %s".format(event, expr))
        }
      }
    } else {
      throw new ConfigurationException("invalid type: %s".format(chanType))
    }
    
    val _categories = if(!config.hasPath("categories")){  
      (event: Event) => Set[String]()
    } else {
      val catType = config.getValue("categories").valueType
      if(catType == ConfigValueType.LIST){
        val set: Set[String] = config.getStringList("categories").toSet
        (event: Event) => set
      } else if(catType == ConfigValueType.OBJECT){
        val expr = Expression(config.getConfig("categories"))
        (event: Event) => expr.evaluate(event) match {
          case Some(l: Traversable[Any]) => set(l)
          case Some(v: Any) => Set[String](v.toString)
          case _ => throw new PublishException("couldn't extract a category from event %s with expression %s".format(event, expr))
        }
      } else {
        throw new ConfigurationException("invalid type: %s".format(catType))
      }
    }
    
    new Publication {
      def channel(event: Event) = _channel(event)
      def categories(event: Event) = _categories(event)
    }
    
  }
  
  def set(l: Traversable[Any]): Set[String] = l match {
    case head :: tail => if (head != null) { TreeSet(head.toString) ++ set(tail) } else { set(tail) } 
    case Nil => TreeSet() 
  }
}

/**
 * Specifies how the event should be published
 */
abstract class Publication {

  def channel(event: Event): String
  def categories(event: Event): Set[String] 
  
}

