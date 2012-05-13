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
package org.evactor.api

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ActorLogging

class ApiServer(
    val system: ActorSystem,
    val port: Int)
  extends Actor with ActorLogging {

  lazy val nettyServer = unfiltered.netty.Http(port).plan(new BasePlan(system))

  def receive = {
    case "startup" => log.info("Starting api..."); nettyServer.run
    case _      	 => nettyServer.stop
  }
  
}
