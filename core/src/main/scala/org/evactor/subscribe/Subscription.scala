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
package org.evactor.subscribe

import scala.collection.JavaConversions.collectionAsScalaIterable

import org.evactor.subscribe.Subscription

import com.typesafe.config.Config

object Subscriptions {

  def apply(): List[Subscription] = List(new Subscription());

  def apply(channel: String): List[Subscription] = List(new Subscription(channel));
  
  def apply(channel: String, category: String): List[Subscription] = List(new Subscription(channel, category));
  
  def apply(subscriptions: java.util.Collection[Subscription]): List[Subscription] = subscriptions.toList
  
  def apply(configs: List[Config]) = configs.map { c =>
    new Subscription(get(c, "channel"), get(c, "category"))
  }
  
  def get(config: Config, name: String): Option[String] = 
    if(config.hasPath(name)){
      Some(config.getString(name))
    } else {
      None
    } 
  
}

case class Subscription(
    val channel: Option[String],
    val category: Option[String]) {
  
  def this() = this(None, None)
  
  def this(channel: String) = this(Some(channel), None)
  
  def this(channel: String, category: String) = this(Some(channel), Some(category))
   
  
}