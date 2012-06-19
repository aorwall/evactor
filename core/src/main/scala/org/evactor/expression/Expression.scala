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

import org.evactor.model.events.Event
import com.typesafe.config.Config
import org.evactor.ConfigurationException

object Expression {
  
  def apply(config: Config): Expression = {
    
    if(config.isEmpty) throw new ConfigurationException("config empty: %s".format(config))
    
    val expr = config.entrySet.iterator.next
    expr.getKey match {
      case "static" => new StaticExpression(expr.getValue.unwrapped)
      case "mvel" => new MvelExpression(expr.getValue.unwrapped.toString)
      case o => throw new ConfigurationException("expression type not recognized: %s".format(o))
    }
    
  }
  
}


abstract class Expression {
  def evaluate(event: Event): Option[Any] 
}
