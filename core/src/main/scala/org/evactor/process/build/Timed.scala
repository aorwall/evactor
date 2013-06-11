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
package org.evactor.process.build

import akka.actor.Actor
import Actor._
import akka.actor.Cancellable
import scala.concurrent.duration._
import org.evactor.model.Timeout
import akka.dispatch.Dispatchers

trait Timed extends Actor  {
  
  import context.dispatcher
  
  def timeout: Option[Long]
      
  var scheduledTimeout:Option[Cancellable] = None 
  
  override def preStart() {
    scheduledTimeout = timeout match {
      case Some(timeout) => Some(context.system.scheduler.scheduleOnce(timeout milliseconds, self, Timeout))
      case _ => None
    }
  }
  
  override def postStop() {
	 scheduledTimeout match {
	    case Some(s) => s.cancel()
	    case _ =>
	 }
  }
  
}
