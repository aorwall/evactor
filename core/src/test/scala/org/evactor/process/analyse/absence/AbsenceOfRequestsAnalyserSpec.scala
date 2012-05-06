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
package org.evactor.process.analyse.absence

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.Event
import org.scalatest.junit.JUnitRunner
import org.evactor.EvactorSpec
import org.evactor.model.Message
import org.evactor.publish.TestPublication

@RunWith(classOf[JUnitRunner])
class AbsenceOfRequestsAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("AbsenceOfRequestsAnalyserSpec"))

  val name = "event"
  val eventName = Some(classOf[Event].getSimpleName + "/eventName")

  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {
      val time = 100L
      val testProbe = TestProbe()

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(Nil, new TestPublication(testProbe.ref), time))

      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }
    
    "alert on timeout from set timeframe plus the time when the latest event arrived " in {
      val time = 200L
      val testProbe = TestProbe()

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(Nil, new TestPublication(testProbe.ref), time))

      Thread.sleep(100)

      actor ! new Message("", Set(), createEvent())

      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }

    "alert on timeout, and send \"back to normal\" message when an event arrives" in {
      val time = 100L
      val testProbe = TestProbe()

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(Nil, new TestPublication(testProbe.ref), time))

      testProbe.expectMsgAllClassOf(300 millis, classOf[AlertEvent])

      actor ! new Message("", Set(), createEvent())
      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent]) // TODO: Need to check back to normal!

      actor.stop()
    }
        
  }

}