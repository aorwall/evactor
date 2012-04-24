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
import com.codahale.jerkson.Json.generate
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
import unfiltered.response.NotFound
import org.evactor.model.events.RequestEvent
import grizzled.slf4j.Logging
import org.evactor.model.events.Event
import scala.io.Source
import org.evactor.model.State
import org.evactor.storage.RequestEventStorage
import unfiltered.response.ResponseFunction
import org.jboss.netty.handler.codec.http.HttpResponse

class RequestEventAPI(val system: ActorSystem) extends EventAPI with Logging {
    
  val storage = EventStorageExtension(system).getEventStorage(classOf[RequestEvent].getName) match {
    case Some(s: RequestEventStorage) => s
    case Some(s) => throw new RuntimeException("Storage impl is of the wrong type: %s".format(s))
    case None => throw new RuntimeException("No storage impl")
  }
     
  override def doRequest(
      path: Seq[String], 
      params: Map[String, Seq[String]]): ResponseFunction[HttpResponse] = path match {
    case "latency" :: channel :: Nil => getAvgLatency(decode(channel), None, getInterval(params.get("interval")))
    case "latency" :: channel :: category :: Nil => getAvgLatency(decode(channel), Some(decode(category)), getInterval(params.get("interval")))
    case _ => super.doRequest(path, params)
  }
  
  protected[api] def getAvgLatency(channel: String, category: Option[String], interval: String): Map[String, Any] = 
    average(storage.getLatencyStatistics(channel, None, Some(0L), Some(now), interval))
  
  override protected[api] def getStats(path: Seq[String], params: Map[String, Seq[String]]): Map[String, Any] =
    path match {
      case channel :: Nil => storage.getStatistics(decode(channel), None, getState(params.get("state")), Some(0L), Some(now), getInterval(params.get("interval")))
   	  case channel :: category :: Nil => storage.getStatistics(decode(channel), Some(decode(category)), getState(params.get("state")), Some(0L), Some(now), getInterval(params.get("interval")))
   	  case e => throw new IllegalArgumentException("Illegal stats request: %s".format(e))
  }
  
  override protected[api] def getEvents(path: Seq[String], params: Map[String, Seq[String]]): List[Map[String, Any]] = 
    path match {
      case channel :: Nil => storage.getEvents(decode(channel), None, getState(params.get("state")), None, None, 10, 0)
   	  case channel :: category :: Nil => storage.getEvents(decode(channel), Some(decode(category)), getState(params.get("state")), None, None, 10, 0)
   	  case e => throw new IllegalArgumentException("Illegal events request: %s".format(e))
  }
  
  override protected[api] def toMap(event: Event): Map[String, Any] = event match {
    case request: RequestEvent => Map ("id" -> request.id, 
         "timestamp" -> request.timestamp,
         "inboundRef" -> request.inboundRef,
         "outboundRef" -> request.outboundRef,
         "state" -> request.state.toString,
         "latency" -> request.latency)   
  }
       
  protected[api] def average ( sum: (Long, List[(Long, Long)])) = 
    Map ("timestamp" -> sum._1, 
         "stats" -> sum._2.map { 
    case (x,y) => if(x > 0) y/x
    					 else 0
  })
  
}

