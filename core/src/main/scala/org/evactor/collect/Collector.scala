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
package org.evactor.collect

import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashSet

import org.evactor.listen.Listener
import org.evactor.model.events.Event
import org.evactor.monitor.Monitored
import org.evactor.publish._
import org.evactor.storage.Storage
import org.evactor.transform.Transformer

import com.typesafe.config.Config

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.duration._

/**
 * Collecting incoming events
 */
class Collector(
    val listener: Option[Config], 
    val transformer: Option[Config], 
    val publication: Publication,
    val timeInMem: Long) 
  extends Actor 
  with Publisher
  with Monitored
  with ActorLogging {
  
  def this(listener: Option[Config], transformer: Option[Config], publication: Publication) =
    this(listener, transformer, publication, 120 * 1000)
  
  val startTime = System.currentTimeMillis
  
  private val ids = new HashSet[String]()
  private var timeline = new TreeMap[Long, String]()
  
  private val dbCheck = context.actorOf(Props(new CollectorStorageCheck(publication)))

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: IllegalArgumentException => Stop
    case _: Exception => Restart
  }
  
  def receive = {
    case event: Event => collect(event)
    case msg => log.debug("can't handle {}", msg)
  }

  def collect(event: Event) {

    log.debug("collecting: {}", event)

    // If timestamp is older than one hour, send to db check
    if(event.timestamp < System.currentTimeMillis - timeInMem || event.timestamp < startTime){
      dbCheck ! event
    } else {
      // otherwise, check memory
      if(!eventExists(event)) {
        incr("collect")
        publish(event)
      } else {
        log.debug("An event with the same id has already been processed: {}", event) 
      }
    } 
  }
  
  protected[collect] def eventExists(event: Event) = {
    val exists = ids.contains(event.id)
    
    // remove old
    val old = timeline.takeWhile( _._1 < System.currentTimeMillis - timeInMem )
    for((_, id) <- old) ids.remove(id)
    timeline = timeline.drop(old.size)
    
    if(!exists){
      ids += event.id
      timeline += (event.timestamp -> event.id)
    }
    exists
  }
  
  override def preStart = { 
    // Start transformer and set collector to it
    val sendTo = transformer match {
      case Some(t) => context.actorOf(Props(Transformer(t, context.self)))
      case None => context.self
    }
    
    // Start listener and set a  transformer to it
    listener match {
      case Some(l) => context.actorOf(Props(Listener(l, sendTo)))
      case None => log.warning("No listener configuration provided to collector!")
    }
    
  }

  override def postStop = {
    // Stop transformer and listener?
  }
}


class CollectorStorageCheck(val publication: Publication) 
  extends Actor 
  with Publisher
  with Storage
  with Monitored
  with ActorLogging {
  
  def receive = {
    case event: Event => collect(event)
    case msg => log.debug("can't handle {}", msg)
  }

  def collect(event: Event) {
    
    log.debug("check for {} in db", event)
   
    if(!eventExists(event)) {
      incr("collect")
      publish(event)
    } else {
      log.warning("An event with the same id has already been processed: {}", event) 
    }
    
  }
  
}

object Collector {
  
  def apply(config: Config): Collector = {
    
    import config._
    
    lazy val pub = Publication(getConfig("publication"))
    lazy val listener = if(hasPath("listener")) Some(getConfig("listener")) else None
    lazy val transformer = if(hasPath("transformer")) Some(getConfig("transformer")) else None
    
    if(hasPath("timeInMem")){
      new Collector(listener, transformer, pub, getMilliseconds("timeInMem"))  
    } else {
      new Collector(listener, transformer, pub)
    }
  }
}