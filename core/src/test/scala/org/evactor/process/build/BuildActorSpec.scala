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
package org.evactor.process.build

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import org.evactor.model.Start
import org.evactor.model.Success
import org.evactor.model.Timeout
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.mockito.Mockito._
import org.evactor.process.ProcessorEventBus
import org.evactor.model.events.Event
import org.evactor.BamSpec

trait TestEventBuilder extends EventBuilder  {  

  def addEvent(event: Event) {}

  def isFinished = true

  def createEvent() = Right(new Event("channel", Some("category"), "id", 0L))

  def clear() {}
    
}

@RunWith(classOf[JUnitRunner])
class BuildActorSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with BamSpec
  with BeforeAndAfterAll
  with BeforeAndAfter {

  def this() = this(ActorSystem("BuildActorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  "A BuildActor" must {

    "add incoming log events to request list " in {
   	
      var added = false
      
      val actor = TestActorRef(new BuildActor("329380921309", 10000) 
      		with TestEventBuilder { 
      				override def isFinished = false 
      				override def addEvent(event: Event) = added = true
      				def timeout = None
      		})

      val logEvent = createLogEvent(0L, Start)

      actor ! logEvent
      added must be (true)
      actor.stop
    }

    "send the activity to analyser when it's finished " in {
      val probe = TestProbe()
      
      val logEvent = new LogEvent("channel", Some("category"), "329380921309", 0L, "329380921309", "client", "server", Start, "hello")
      val reqEvent = new RequestEvent("channel", Some("category"), "329380921309", 0L, None, None, Success, 0L)
      
      val actor = TestActorRef(new BuildActor("correlationId", 1000)
      		with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(reqEvent)
      				def timeout = None
      		})

      actor ! probe.ref
      actor ! logEvent	     

      probe.expectMsg(1 seconds, reqEvent) // The activity returned by activityBuilder should be sent to activityPrope
      
      println(actor.isTerminated)
      
      actor.stop
    }

    "send an activity with status TIMEOUT to analyser when timed out" in {
      val probe = TestProbe()

      val timedoutEvent = new RequestEvent("channel", Some("category"), "329380921309", 0L, None, None, Timeout, 0L)

      val timeoutEventActor = TestActorRef(new BuildActor("329380921309", 100) with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(timedoutEvent)
      		})

      
      val logEvent = new LogEvent("channel", Some("category"), "329380921309", 0L, "329380921309", "client", "server", Start, "hello")
      timeoutEventActor ! probe.ref
      timeoutEventActor ! logEvent

      probe.expectMsg(1 seconds, timedoutEvent)

      timeoutEventActor.stop
    }
  }
}