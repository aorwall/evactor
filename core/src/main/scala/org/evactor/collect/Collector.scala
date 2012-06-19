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

import java.util.TreeMap
import scala.collection.JavaConversions._
import org.evactor.listen.Listener
import org.evactor.listen.ListenerException
import org.evactor.model.events.Event
import org.evactor.monitor.Monitored
import org.evactor.publish.Publication
import org.evactor.publish.Publisher
import org.evactor.storage.Storage
import org.evactor.transform.Transformer
import org.evactor.ConfigurationException
import com.typesafe.config.Config
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.SupervisorStrategy.Restart
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.util.duration.intToDurationInt
import scala.collection.mutable.HashSet
import akka.actor.ActorInitializationException

/**
 * Collecting incoming events
 */
class Collector(
    val listener: Option[Config], 
    val transformer: Option[Config], 
    val publication: Publication,
    val timeInMem: Long = 600 * 1000) 
  extends Actor 
  with Publisher
  with Monitored
  with ActorLogging {
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e: ConfigurationException => Escalate
    case e: ActorInitializationException => Escalate
    case e: ListenerException => log.error("Caught exception: {}", e); Restart
    case e: Exception => log.error("Caught exception: {}", e); Restart
  }
  
  val startTime = System.currentTimeMillis
  
  private val ids = new HashSet[String]
  private val timeline = new TreeMap[Long, List[String]]()
  
  private val dbCheck = context.actorOf(Props(new CollectorStorageCheck(publication)))
  
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
    
    // remove old
    val oldTime = System.currentTimeMillis - timeInMem
    
    val old = timeline.headMap(oldTime)
    for((timestamp, timedIds) <- old) {
      for(timedId <- timedIds){
        ids -= timedId
      }
      
      timeline.remove(timestamp)
    }
    
    val exists = ids.contains(event.id)
    if(!exists && event.timestamp > oldTime){
      ids += event.id
      
      val timedIds = if(timeline.containsKey(event.timestamp)){
        event.id :: timeline.get(event.timestamp)
      } else {
        List(event.id)
      }
      
      timeline.put(event.timestamp, timedIds)
    }
    exists
  }
  
  override def preStart = { 
    // Start transformer and set collector to it
    val sendTo = transformer match {
      case Some(t) => context.actorOf(Props(Transformer(t, context.self)), name="transformer")
      case None => context.self
    }
    
    // Start listener and set a  transformer to it
    listener match {
      case Some(l) => context.actorOf(Props(Listener(l, sendTo)), name="listener")
      case None => log.warning("No listener configuration provided to collector!")
    }
    
    super.preStart()
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