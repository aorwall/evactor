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
package org.evactor.process

import akka.actor.ActorRef

/**
 * TODO: The right name for this?
 */
abstract class Publication {

  def channel: String
  def category: Option[String] 
  
}

case class StaticPublication (
    val _channel: String,
    val _category: Option[String]) extends Publication {
    
  def channel = _channel
  def category = _category
    
}

case class TestPublication (val testActor: ActorRef) extends Publication {
    
  def channel = "none"
  def category = None
    
}