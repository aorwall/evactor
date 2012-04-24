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
import org.evactor.model.events.KpiEvent
import org.evactor.storage.KpiEventStorage
import unfiltered.response.ResponseFunction
import org.jboss.netty.handler.codec.http.HttpResponse
import org.evactor.model.events.Event

class KpiEventAPI (override val system: ActorSystem) extends EventAPI {
    
  val storage = EventStorageExtension(system).getEventStorage(classOf[KpiEvent].getName) match {
    case Some(s: KpiEventStorage) => s
    case Some(s) => throw new RuntimeException("Storage impl is of the wrong type: %s".format(s))
    case None => throw new RuntimeException("No storage impl found for KPI Event")
  }
     
  override def doRequest(
      path: Seq[String], 
      params: Map[String, Seq[String]]): ResponseFunction[HttpResponse] = path match {
    case "avg" :: channel :: Nil => getAverage(decode(channel), None, getInterval(params.get("interval")))
    case "avg" :: channel :: category :: Nil => getAverage(decode(channel), Some(decode(category)), getInterval(params.get("interval")))
    case _ => super.doRequest(path, params)
  }
  
  protected def getAverage(channel: String, category: Option[String], interval: String) = {
    average(storage.getSumStatistics(channel, category, Some(0L), Some(now), interval))
  }
  
  protected def average ( sum: (Long, List[(Long, Double)])) = (sum._1, sum._2.map { 
	  case (x,y) => if(x > 0) y/x
                  else 0
	})
	
  override implicit protected[api] def toMap(e: Event): Map[String, Any] = e match {
    case event: KpiEvent => Map ("id" -> event.id, 
         "timestamp" -> event.timestamp,
         "value" -> event.value)
  }
  
}

