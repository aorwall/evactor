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
package org.evactor.process.build.request

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.Start
import org.evactor.model.Success
import org.evactor.model.Failure
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.evactor.process.build.BuildActor
import org.evactor.EvactorSpec

@RunWith(classOf[JUnitRunner])
class RequestBuilderSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with EvactorSpec {

  def this() = this(ActorSystem("RequestBuilderSpec"))
  
  val compId = "startComponent"
  
  val actor = TestActorRef(new RequestBuilder(Nil, 0L))
  val processor = actor.underlyingActor

  "A RequestProcessor" must {

    "should always return true when handlesEvent is called " in {
      processor.handlesEvent(createLogEvent(0L, Start)) must be === true
    }

  }

  "A RequestEventBuilder" must {

    "create a RequestEvent with state SUCCESS when a component is succesfully processed" in {
      val buildActor = TestActorRef(new BuildActor("329380921309", 1000) 
      		with RequestEventBuilder { 
      			def timeout = Some(1000L)
      		})
      	
      val eventBuilder = buildActor.underlyingActor
      		
      eventBuilder.addEvent(createLogEvent(0L, Start))
      eventBuilder.addEvent(createLogEvent(0L, Success))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case Right(r: RequestEvent) => r.state must be(Success)
        case _ => fail()
      }     
    }

    "create a RequestEvent with state FAILURE when the LogEvent to the component has state FAILURE" in {
       val buildActor = TestActorRef(new BuildActor("329380921309", 1000) 
      		with RequestEventBuilder { 
      			def timeout = Some(1000L)
      		})
      	
      val eventBuilder = buildActor.underlyingActor
      
      eventBuilder.addLogEvent(createLogEvent(0L, Start))
      eventBuilder.addLogEvent(createLogEvent(0L, Failure))
      eventBuilder.isFinished must be === true
      
      eventBuilder.createEvent() match {
        case Right(r: RequestEvent) => r.state must be(Failure)
        case _ => fail()
      }     
    }

    
  }

}