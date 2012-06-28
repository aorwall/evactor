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

import java.net.URLDecoder

import org.evactor.model.events.Event
import org.evactor.model.State
import org.evactor.storage.EventStorage
import org.evactor.storage.EventStorageExtension
import org.evactor.storage.KpiStorage
import org.evactor.storage.LatencyStorage
import org.evactor.storage.StateStorage

import com.fasterxml.jackson.module.scala.DefaultScalaModule

import akka.actor.ActorSystem
import unfiltered.response._
import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer, SerializationFeature, ObjectMapper}
import scala.Some
import unfiltered.response.ResponseString
import javax.servlet.http.HttpServletResponse
import grizzled.slf4j.Logging
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.core.JsonGenerator

class EventAPI (val system: ActorSystem) extends Logging {


  val mapper = {
    val m = new ObjectMapper()

    val stateModule = new SimpleModule()
    stateModule.addSerializer(classOf[State], new JsonSerializer[State] {
      def serialize(state: State, jgen: JsonGenerator, provider: SerializerProvider) {jgen.writeString(state.name)}
    })

    m.registerModule(stateModule)
    m.registerModule(DefaultScalaModule)

    m
  }

  // TODO: Some other system to decide which storage solutions that is available.
  // Maybe a LatencyAPI and StateAPI trait extending this one
  val storage: EventStorage with LatencyStorage with StateStorage with KpiStorage = EventStorageExtension(system).getEventStorage() match {
    case Some(s: EventStorage with LatencyStorage with StateStorage with KpiStorage) => s
    case Some(s) => throw new RuntimeException("Storage impl is of the wrong type: %s".format(s))
    case None => throw new RuntimeException("No storage impl found")
  }
  
  def doRequest(
      path: Seq[String], 
      params: Map[String, Seq[String]]): ResponseFunction[HttpServletResponse] =
    path match {
      case "channels" :: Nil => getChannels(getCount(params.get("count"), 100))
      case "categories" :: channel :: Nil => getCategories(decode(channel), getCount(params.get("count"), 100))
      case "stats" :: tail => getStats(tail, params)
      case "timeline" :: tail => getTimeline(tail, params)
      case "event" :: id :: Nil => getEvent(id) 
      case "count" :: tail => getCount(tail, params)
      case "latency" :: channel :: Nil => getAvgLatency(decode(channel), None, getInterval(params.get("interval")))
      case "latency" :: channel :: category :: Nil => getAvgLatency(decode(channel), Some(decode(category)), getInterval(params.get("interval")))
      case "avg" :: channel :: Nil => getAverage(decode(channel), None, getInterval(params.get("interval")))
      case "avg" :: channel :: category :: Nil => getAverage(decode(channel), Some(decode(category)), getInterval(params.get("interval")))
      case _ => BadRequest ~> ResponseString("Unknown resource...")
  }
  
  protected[api] def getChannels(count: Int): List[Map[String, Any]] = 
    storage.getEventChannels(count)
  
  protected[api] def getCategories(channel: String, count: Int): List[Map[String, Any]] = 
  	storage.getEventCategories(decode(channel), count)

  protected[api] def getStats(path: Seq[String], params: Map[String, Seq[String]]): Map[String, Any] =
    path match {
      case channel :: Nil => storage.getStatistics(decode(channel), None, getState(params.get("state")), Some(0L), Some(now), getInterval(params.get("interval")))
   	  case channel :: category :: Nil => storage.getStatistics(decode(channel), Some(decode(category)), getState(params.get("state")), Some(0L), Some(now), getInterval(params.get("interval")))
   	  case e => throw new IllegalArgumentException("Illegal stats request: %s".format(e))
  }
  
  protected[api] def getTimeline(path: Seq[String], params: Map[String, Seq[String]]): List[Event] = {
    val from = getLongOption(params.get("from"))
    val to = getLongOption(params.get("to"))

    val count = params.get("count").getOrElse("0")
    
    path match {
      case channel :: Nil => storage.getEvents(decode(channel), None, getState(params.get("state")), from, to, 10, 0)
      case channel :: category :: Nil => storage.getEvents(decode(channel), Some(decode(category)), getState(params.get("state")), from, to, 10, 0)
      case e => throw new IllegalArgumentException("Illegal events request: %s".format(e))
    }
  }
  
  protected[api] def getCount(path: Seq[String], params: Map[String, Seq[String]]): Long = {
    val from = getLongOption(params.get("from"))
    val to = getLongOption(params.get("to"))
    path match {
      case channel :: Nil => storage.count(decode(channel), None, getState(params.get("state")), from, to)
      case channel :: category :: Nil => storage.count(decode(channel), Some(decode(category)), getState(params.get("state")), from, to)
      case e => throw new IllegalArgumentException("Illegal events request: %s".format(e))
    } 
  }
  
  protected[api] def getLongOption(in: Option[Seq[String]]): Option[Long] = in match{
    case Some(s) => Some(s.toString.toLong)
    case _ => None
  }
  
  protected[api] def getEvent(id: String): Option[Event] = 
    storage.getEvent(id) match {
      case Some(e: Event) => Some(e)
      case _ => None
  }
  
  protected[api] def getAvgLatency(channel: String, category: Option[String], interval: String): Map[String, Any] = 
    average(storage.getLatencyStatistics(channel, None, Some(0L), Some(now), interval))
  
  protected def getAverage(channel: String, category: Option[String], interval: String) = {
    average(storage.getSumStatistics(channel, category, Some(0L), Some(now), interval))
  }
  
  protected[api] def average ( sum: (Long, List[(Long, Long)])) = 
    Map ("timestamp" -> sum._1, 
         "stats" -> sum._2.map { 
    case (x,y) => if(x > 0) y/x
                  else 0
  })
  
  implicit protected[api] def anyToResponse(any: Any): ResponseFunction[HttpServletResponse] = any match {
    case None => NotFound
    case Some(obj) => ResponseString(mapper.writeValueAsString(obj))
    case _ => ResponseString(mapper.writeValueAsString(any))
  }
  
  protected[api] def decode(name: String) = {
    URLDecoder.decode(name, "UTF-8")
  }
  
  protected[api] def getCount(count: Option[Seq[String]], default: Int): Int = count match {
    case Some(s) => s.mkString.toInt 
    case None => default
  }
  
  protected[api] def getInterval (interval: Option[Seq[String]]) = interval match {
    case Some( i :: Nil) => i
    case None => "day"
  }

  protected[api] def getState (state: Option[Seq[String]]) = state match {
    case Some(s) => Some(State.apply(s.mkString))
    case None => None
  }
  
  implicit protected[api] def toCountMap(list: List[(String, Long)]): List[Map[String, Any]] =
    list.map { (t) => Map("name" -> t._1, "count" -> t._2) }
  
  implicit protected[api] def toStatsMap(stats: (Long, List[Long])): Map[String, Any] = 
    Map ("timestamp" -> stats._1, "stats" -> stats._2)
    
  protected[api] def now = System.currentTimeMillis
  
}