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

import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.Event
import javax.xml.xpath.XPathFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import java.io.ByteArrayInputStream

/**
 * Evaluate XPath Expressions. 
 * 
 * Just returning strings...
 */
case class XPathExpression(val expression: String) extends Expression {

  val factory = XPathFactory.newInstance
  val xpath = factory.newXPath
  lazy val xpathExpr = xpath.compile(expression) 
  
  val domFactory = DocumentBuilderFactory.newInstance();
  //domFactory.setNamespaceAware(true); 
  val builder = domFactory.newDocumentBuilder(); 
  
  override def evaluate(e: Event): Option[String] = e match {
    case event: Event with HasMessage => {    
      try {
        val doc = builder.parse(new ByteArrayInputStream(event.message.getBytes())) //"UTF-8"
        val result = xpathExpr.evaluate(doc, XPathConstants.STRING)
  
        result match {
          case "" => None
          case s: String => Some(s)
          case _ => None
        }
      } catch {
        case e: Exception => None 
      }
    }
    case _ => None // TODO: Throw exception?
  }

}

