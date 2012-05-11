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
package org.evactor.process.analyse.count

import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.evactor.EvactorSpec
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestProbe
import akka.testkit.TestActorRef
import org.evactor.expression.MvelExpression
import org.evactor.expression.StaticExpression
import org.evactor.model.events.Event
import akka.util.duration._
import org.evactor.model.events.AlertEvent
import org.evactor.model.Message
import org.evactor.model.events.DataEvent
import org.evactor.publish.TestPublication
import org.evactor.process.CategoryProcessor

@RunWith(classOf[JUnitRunner])
class CountAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("CountAnalyserSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A WordCounter" must {

    "alert if a word occured more than a specified amount of times in a specified time frame" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Nil, new TestPublication(testProbe.ref), false, 1, 100 ))
      actor ! new Message("channel", Set(), new Event("id1", System.currentTimeMillis ))
      actor ! new Message("channel", Set(), new Event("id2", System.currentTimeMillis ))
      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }
    
    "alert twice if different words occured more than max occurences" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Nil, new TestPublication(testProbe.ref), true, 1, 100 ))
      actor ! new Message("channel", Set("a"), new DataEvent("id1", System.currentTimeMillis, "a" ))
      actor ! new Message("channel", Set("b"), new DataEvent("id2", System.currentTimeMillis+1, "b" ))
      actor ! new Message("channel", Set("a"), new DataEvent("id3", System.currentTimeMillis+2, "a" ))
      actor ! new Message("channel", Set("b"), new DataEvent("id4", System.currentTimeMillis+3, "b" ))
      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }
    
    "stop word sub counter on timeout " in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Nil, new TestPublication(testProbe.ref), false, 1, 100 ))
      val count = actor.underlyingActor
      actor ! new Message("channel", Set(), new DataEvent("id1", System.currentTimeMillis, "http://foo" ))
      Thread.sleep(150)
      testProbe.expectNoMsg
      actor ! new Message("channel", Set(), new DataEvent("id3", System.currentTimeMillis, "http://foo" ))
      actor ! new Message("channel", Set(), new DataEvent("id4", System.currentTimeMillis, "http://foo" ))
      testProbe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }
  }
}