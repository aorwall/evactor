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
import scala.collection.mutable.HashMap
import org.evactor.listen.Listener
import org.evactor.listen.ListenerConfiguration
import org.evactor.model.events.Event
import org.evactor.monitor.Monitored
import org.evactor.process.Processor
import org.evactor.publish._
import org.evactor.storage.Storage
import org.evactor.transform.Transformer
import org.evactor.transform.TransformerConfiguration
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.duration._
import scala.collection.mutable.HashSet


//import com.twitter.ostrich.stats.Stats

/**
 * Collecting incoming events
 */
class Collector(
    val listener: Option[ListenerConfiguration], 
    val transformer: Option[TransformerConfiguration], 
    val publication: Publication) 
  extends Actor 
  with Publisher
  with Monitored
  with ActorLogging {
  
  val timeInMem = 3600 * 1000; // Save all id's in memory for one hour
  val startTime = System.currentTimeMillis
  
  private val ids = new HashSet[String]()
  private var timeline = new TreeMap[Long, String]()
  
  private val dbCheck = context.actorOf(Props(new CollectorDbCheck(publication)))

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
        log.warning("An event with the same id has already been processed: {}", event) 
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
      case Some(t) => context.actorOf(Props(t.transformer(context.self)))
      case None => context.self
    }
    
    // Start listener and set a  transformer to it
    listener match {
      case Some(l) => context.actorOf(Props(l.listener(sendTo)))
      case None => log.warning("No listener")
    }
    
  }

  override def postStop = {
    // Stop transformer and listener?
  }
}


class CollectorDbCheck(val publication: Publication) 
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