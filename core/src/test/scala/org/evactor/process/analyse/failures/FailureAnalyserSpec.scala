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
package org.evactor.process.analyse.failures

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import akka.util.duration.longToDurationLong
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.LogEvent
import org.evactor.model
import org.evactor.model.Success
import org.evactor.process.analyse.window.TimeWindow
import org.evactor.EvactorSpec
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.process.StaticPublication
import org.evactor.process.TestPublication
import org.evactor.model.Message

@RunWith(classOf[JUnitRunner])
class FailureAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("FailureAnalyserSpec"))

  val name = "name"
  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A FailureAnalyser" must {

    "alert when the number of failed incoming events exceeds max allowed failures" in {
      val testProbe = TestProbe()
      val failureActor = TestActorRef(new FailureAnalyser(Nil, new TestPublication(testProbe.ref), 2))

      failureActor ! new Message("", Set(), createLogEvent(0L, Success))
      failureActor ! new Message("", Set(), createLogEvent(1L, model.Failure))
      failureActor ! new Message("", Set(), createLogEvent(2L, model.Failure))
      testProbe.expectNoMsg // nothing happens
      failureActor ! new Message("", Set(), createLogEvent(3L, model.Failure)) //  trig alert!

      //probe.expectMsg(100 millis, new AlertEvent(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true)) TODO FIX!
      testProbe.expectMsgAllClassOf(400 millis, classOf[AlertEvent])

      failureActor.stop()
    }

    "alert when the number of failed incoming events exceeds max within a specified time window" in {

      val time = 100L
      val currentTime = System.currentTimeMillis()
      val testProbe = TestProbe()

      val failureActor = TestActorRef(new FailureAnalyser(Nil, new TestPublication(testProbe.ref), 2) with TimeWindow {override val timeframe = time} )

      failureActor ! new Message("", Set(), createLogEvent(currentTime-50, model.Failure))
      failureActor ! new Message("", Set(), createLogEvent(currentTime-40, model.Failure))
      failureActor ! new Message("", Set(), createLogEvent(currentTime-1000, model.Failure)) // to old, nothing happens
      failureActor ! new Message("", Set(), createLogEvent(currentTime-30, model.Failure))
      //  probe.expectMsg(time*2 millis, new Alert(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true)) TODO FIX!
      testProbe.expectMsgAllClassOf(400 millis, classOf[AlertEvent])

      failureActor.stop
    }

    "alert when the number of failed incoming events exceeds within a specified length window" in {
      val testProbe = TestProbe()
      val latencyActor = TestActorRef(new FailureAnalyser(Nil, new TestPublication(testProbe.ref), 1) with LengthWindow {
        override val noOfRequests = 2
      })

      latencyActor ! new Message("", Set(), createRequestEvent(1L, None, None, "corr", "comp", model.Failure, 10)) 
      latencyActor ! new Message("", Set(), createRequestEvent(2L, None, None, "corr", "comp", model.Failure, 110)) // trig alert!

//      probe.expectMsg(100 millis, new Alert(eventName, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))
      testProbe.expectMsgAllClassOf(50 millis, classOf[AlertEvent])

      latencyActor ! new Message("", Set(), createRequestEvent(4L, None, None, "corr", "comp", Success, 60))
      latencyActor ! new Message("", Set(), createRequestEvent(4L, None, None, "corr", "comp", Success, 60)) // back to normal

 //     probe.expectMsg(100 millis, new Alert(eventName, "back to normal!", false))
      testProbe.expectMsgAllClassOf(50 millis, classOf[AlertEvent])

      latencyActor.stop
    }

  } 
}