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
import com.codahale.jerkson.Json._
import org.evactor.model.events.DataEvent
import org.evactor.storage.EventStorageExtension
import org.evactor.storage.Storage
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ResponseString
import org.codehaus.jackson.map.ObjectMapper
import java.net.URLEncoder
import java.net.URLDecoder
import unfiltered.request.Params
import unfiltered.response.BadRequest
import org.evactor.model.events.AlertEvent
import org.evactor.storage.EventStorage
import grizzled.slf4j.Logging
import org.evactor.model.events.Event

class AlertEventAPI(val system: ActorSystem) extends EventAPI {
    
  lazy val storage = EventStorageExtension(system).getEventStorage(classOf[AlertEvent].getName) match {
    case Some(s: EventStorage) => s
    case None => throw new RuntimeException("No storage impl")
  }
  
  override implicit protected[api] def toMap(e: Event): Map[String, Any] = e match {
    case event: AlertEvent =>  Map ("id" -> event.id, 
         "timestamp" -> event.timestamp,
         "triggered" -> event.triggered,
         "eventRef" -> event.eventRef,
         "message" -> event.message)   
  }
   
   
}

