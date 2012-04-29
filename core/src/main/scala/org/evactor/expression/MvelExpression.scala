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
package org.evactor.expression

import scala.Double._
import scala.collection.JavaConversions.asJavaSet
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import akka.actor.ActorLogging
import org.codehaus.jackson.map.ObjectMapper
import org.mvel2.MVEL
import java.util.HashMap

/**
 * Evaluate MVEL Expressions. Supports JSON and strings in message. XML to come...?
 * 
 * Maybe change so all events can be provided... and use abstract type instead of String
 * 
 */
case class MvelExpression(val expression: String) extends Expression {

  lazy val compiledExp = MVEL.compileExpression(expression); 

  override def evaluate(event: Event with HasMessage): Option[String] = {
    
    val obj = new HashMap[String,Any]
    
    // assume json if message starts and ends with curly brackets
    val msg = if(event.message.startsWith("{") && event.message.endsWith("}")){
      val mapper = new ObjectMapper
      
      try {
      	Some(mapper.readValue(event.message, classOf[HashMap[String,Object]]))
      } catch {
        case _ => None
      }
    } else {
      None
    }
              
    obj.put("id", event.id)
    obj.put("timestamp", event.timestamp)
    
    msg match {
      case Some(map) => obj.put("message", map)
      case _ => obj.put("message", event.message)
    }
    
    
    val result = try {
      MVEL.executeExpression(compiledExp, obj)
    } catch {
      case e => None
    }
    
    result match {
      case v: Any => Some(v.toString)
      case _ => None
    }    
  }
}

