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
package org.evactor.test

import scala.util.Random
import java.util.UUID
import akka.actor.{ActorRef, Actor, Props, ActorLogging}
import org.evactor.model._
import org.evactor.model.events.LogEvent
import org.evactor.process.build.simpleprocess.SimpleProcess

class RequestGenerator (
    channels: List[String], 
    collector: ActorRef,
    counter: ActorRef,
    noOfRequests: Int = 1,
    timeBetweenProcesses: Int = 500,
    timeBetweenRequests: Int = 100)
  extends Actor {
  
  val requestActor = context.actorOf(Props(new RequestActor(channels, collector, counter, noOfRequests)).withDispatcher("pinned-dispatcher"), name = "request")   
  
  def receive = {
    case _ => requestActor ! None
  }
  
}

class RequestActor(
    channels: List[String], 
    collector: ActorRef, 
    counter: ActorRef, 
    noOfRequests: Int = 1,
    timeBetweenProcesses: Int = 500,
    timeBetweenRequests: Int = 100) 
  extends Actor 
  with ActorLogging {

  def receive = {
    case _ => {
      for(x <- 0 until noOfRequests) {
        log.debug("processing requests for " + channels)
        sendRequests() 
        counter ! 1
        randomSleep(timeBetweenProcesses)    
      }
    }
  }

  private[this] def sendRequests() {

    for (channel <- channels) {
    	val correlationId = UUID.randomUUID().toString
      val start = new LogEvent(channel, None, correlationId, System.currentTimeMillis, correlationId, "client", "server", Start, "hello")
      log.debug("send: " + start)
      collector ! start
      val state: State = if (oneIn(50) ) Failure else Success

      randomSleep(timeBetweenRequests)
      val stop = new LogEvent(channel, None, correlationId, System.currentTimeMillis, correlationId, "client", "server", state, "goodbye")
      			  
      log.debug("send: " + stop)
      collector ! stop
      
      randomSleep(timeBetweenRequests)
    }
  }
  
  private[this] def randomSleep (time: Int) {
    Thread.sleep(Random.nextInt(time) + 1L)
  }
  
  private[this] def oneIn (x: Int) = Random.nextInt(x) == 0
  
}