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
package org.evactor.process.build.simple

import org.evactor.process.build.simpleprocess.SimpleProcessBuilder
import org.evactor.process.Subscription
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.evactor.process.StaticPublication
import org.evactor.process.build.BuildActor
import org.evactor.process.build.simpleprocess.SimpleProcessEventBuilder
import org.evactor.model.events.RequestEvent
import org.evactor.model.Success
import org.evactor.model.events.SimpleProcessEvent
import org.evactor.model.Failure

@RunWith(classOf[JUnitRunner])
class SimpleProcessSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec {

  def this() = this(ActorSystem("SimpleProcessSpec"))
  
  val processId = "process"
  val startCompId = "startComponent"
  val endCompId = "endComponent"

  val subscriptions = List(new Subscription(Some(startCompId), None), new Subscription(Some(startCompId), None))  
    
  val actor = TestActorRef(new SimpleProcessBuilder(subscriptions, new StaticPublication(processId, Set()), List(startCompId, endCompId), 120000L))
  val processor = actor.underlyingActor


  "A SimpleProcessBuilder" must {

    "create an event with state SUCCESS when flow is succesfully processed" in {
      
      val buildActor = TestActorRef(new BuildActor("corrId", 1000, new StaticPublication("", Set())) 
        with SimpleProcessEventBuilder { 
     	  val components = List(startCompId, endCompId)
        })
      
      val eventBuilder = buildActor.underlyingActor
      eventBuilder.addEvent(new RequestEvent("329380921309", 0L, "corrId", startCompId, None, None, Success, 0L))
      eventBuilder.isFinished must be === false
      eventBuilder.addEvent(new RequestEvent("329380921310", 1L, "corrId", endCompId, None, None, Success, 0L))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case Right(r: SimpleProcessEvent) => r.state must be(Success)
        case _ => fail()
      }  
    }

    "create an activity with state FAILURE when a log event has the state FAILURE" in {
      
      val buildActor = TestActorRef(new BuildActor("corrId", 1000, new StaticPublication("", Set())) 
      		with SimpleProcessEventBuilder { 
              val components = List(startCompId, endCompId)
      		})
      
      val eventBuilder = buildActor.underlyingActor      
      eventBuilder.addEvent(new RequestEvent("329380921309", 0L, "corrId", startCompId, None, None, Failure, 0L))
      eventBuilder.isFinished must be === true

       eventBuilder.createEvent() match {
        case Right(r: SimpleProcessEvent) => r.state must be(Failure)
        case _ => fail()
      }  
    }
    
    "create an event with state SUCCESS when flow with just one component succesfully processed" in {
      
      val buildActor = TestActorRef(new BuildActor("corrId", 1000, new StaticPublication("", Set())) 
      		with SimpleProcessEventBuilder { 
      			val components = List(startCompId)
      		})
      
      val eventBuilder = buildActor.underlyingActor
      eventBuilder.addEvent(new RequestEvent("329380921309", 0L, "corrId", startCompId, None, None, Success, 0L))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case Right(r: SimpleProcessEvent) => r.state must be(Success)
        case _ => fail()
      }  
    }
  }
}