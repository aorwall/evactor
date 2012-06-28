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

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import unfiltered.response.{JsonContent, BadRequest, ResponseString}
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.request.Params
import unfiltered.filter.Plan

class BasePlan extends Plan with Logging {

   val api = {
     lazy val system = ActorSystem("EvactorApi")
     new EventAPI(system)
   }

  def intent = {
    case req @ Path(Seg("api" :: path)) => try {
      val Params(params) = req
      JsonContent ~> api.doRequest(path, params)
    } catch { case e => warn("error while calling event api", e); BadRequest }
    case _ => ResponseString("Couldn't handle request")
      
  }
  
}