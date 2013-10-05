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

import java.util.HashMap
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import org.mvel2.MVEL
import com.fasterxml.jackson.databind.ObjectMapper
import org.mvel2.util.FastList
import scala.collection.JavaConversions.{SeqWrapper, seqAsJavaList}
import scala.reflect.ClassTag
import scala.collection.convert.Wrappers

/**
 * Evaluate MVEL Expressions. Supports JSON and strings in message. XML to come...?
 * 
 * Maybe change so all events can be provided... and use abstract type instead of String
 * 
 */
case class MvelExpression(val expression: String) extends Expression {

  lazy val compiledExp = MVEL.compileExpression(expression); 

  override def evaluate(event: Event): Option[Any] = {
    
    val obj = getCaseClassParams(event) // add all variables from the case class to the map
    // extra attention to Event's with HasMessage trait
    
    event match {
      case h: HasMessage => {
        // assume json if message starts and ends with curly brackets
        val msg = if(h.message.startsWith("{") && h.message.endsWith("}")){
          val mapper = new ObjectMapper
          
          try {
            Some(mapper.readValue(h.message, classOf[HashMap[String,Object]]))
          } catch {
            case _: Throwable => None
          }
        } else {
          None
        }
    
        
        msg match {
          case Some(map) => obj.put("message", map)
          case _ => obj.put("message", h.message)
        }
      }
      case _ =>
    }
              
    obj.put("id", event.id)
    obj.put("timestamp", event.timestamp)

    val result = try {
      MVEL.executeExpression(compiledExp, obj) match {
        case Wrappers.SeqWrapper(f) => Some(f.toList)
        case f: org.mvel2.util.FastList[Any] => Some(f.toArray.toList)
        case a: Any => Some(a)
      }
    } catch {
      case _: Throwable => None
    }
    
    result
  }
  
  private[this] def getCaseClassParams(cc: AnyRef) = {
    val map = new HashMap[String, Any]()
    
    val fields = cc.getClass.getDeclaredFields
    
    fields.foreach { f =>
      f.setAccessible(true)

      f.get(cc) match {
        case Some(o) => map.put(f.getName, o)
        case None => // do nothing
        case l: Iterable[Any] => map.put(f.getName, seqAsJavaList(l.toList))
        case o => map.put(f.getName, o)
      }
      
    }

    map
  }
  

}

