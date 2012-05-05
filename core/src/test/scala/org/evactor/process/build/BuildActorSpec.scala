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

import org.evactor.model.events.Event
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.Message
import org.evactor.process.StaticPublication
import org.evactor.process.TestPublication
import org.evactor.EvactorSpec
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

trait TestEventBuilder extends EventBuilder  {  

  def addEvent(event: Event) {}

  def isFinished = true

  def createEvent() = Right(new Event("id", 0L))

  def clear() {}
    
}

@RunWith(classOf[JUnitRunner])
class BuildActorSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with EvactorSpec
  with BeforeAndAfterAll
  with BeforeAndAfter {

  def this() = this(ActorSystem("BuildActorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  "A BuildActor" must {

    "add incoming log events to request list " in {
   	
      var added = false
      
      val actor = TestActorRef(new BuildActor("329380921309", 10000, new StaticPublication("", Set())) 
      		with TestEventBuilder { 
      				override def isFinished = false 
      				override def addEvent(event: Event) = added = true
      		})

      val logEvent = createLogEvent(0L, Start)

      actor ! logEvent
      added must be (true)
      actor.stop
    }

    "send the activity to analyser when it's finished " in {
      val probe = TestProbe()
      
      val logEvent = new LogEvent("329380921309", 0L, "329380921309", "comp", "client", "server", Start, "hello")
      val reqEvent = new RequestEvent("329380921309", 0L, "329380921309", "comp", None, None, Success, 0L)
      val reqEventMsg = new Message("channel", Set("category"), reqEvent)
      
      val actor = TestActorRef(new BuildActor("correlationId", 1000, new TestPublication(probe.ref))
      		with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(reqEvent)
      		})

      actor ! logEvent	     

      probe.expectMsg(1 seconds, reqEvent) // The activity returned by activityBuilder should be sent to activityPrope
      
      println(actor.isTerminated)
      
      actor.stop
    }

    "send an activity with status TIMEOUT to analyser when timed out" in {
      val probe = TestProbe()

      val timedoutEvent = new RequestEvent("329380921309", 0L, "corrId", "comp", None, None, Timeout, 0L)

      val timeoutEventActor = TestActorRef(new BuildActor("329380921309", 100, new TestPublication(probe.ref)) with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(timedoutEvent)
      		})

      
      val logEvent = new LogEvent("329380921309", 0L, "329380921309", "comp", "client", "server", Start, "hello")
      val logEventMsg = new Message("channel", Set("category"), logEvent)
      timeoutEventActor ! logEvent

      probe.expectMsg(1 seconds, timedoutEvent)

      timeoutEventActor.stop
    }
  }
}