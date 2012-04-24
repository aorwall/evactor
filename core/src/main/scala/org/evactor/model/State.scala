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
package org.evactor.model

object State {
  
  val START = "START"
  val RETRY = "RETRY"
  val SUCCESS = "SUCCESS"
  val FAILURE = "FAILURE"
  val CANCELLATION = "CANCELLATION"
  val TIMEOUT = "TIMEOUT"
  
  def apply(state: String): State = state match {
    case START => Start
    case RETRY => Retry
    case SUCCESS => Success
    case FAILURE => Failure
    case CANCELLATION => Cancellation
    case TIMEOUT => Timeout
    case _ => throw new IllegalArgumentException("Couldn't create a state instance with argument: " + state)
  }
  
}

sealed trait State { 
  def name: String 
  override def toString = name
}

case object Start extends State { val name = State.START } 

case object Retry extends State { val name = State.RETRY }

case object Success extends State { val name = State.SUCCESS }

case object Failure extends State { val name = State.FAILURE }

case object Cancellation extends State { val name = State.CANCELLATION }

case object Timeout extends State { val name = State.TIMEOUT }
