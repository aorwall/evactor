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
import org.evactor.model.events.ValueEvent
import akka.actor.ActorRef
import akka.actor.Actor
import org.evactor.subscribe.Subscriptions

@RunWith(classOf[JUnitRunner])
class CountAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("CountAnalyserSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A CountAnalyser" must {

    "count all events that occured within a specified time frame" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Subscriptions("channel"), new TestPublication(valueDest(testProbe.ref)), false, 500 ), name="test1")
      actor ! new Message("channel", Set(), new Event("id1", System.currentTimeMillis ))
      testProbe.expectMsg(300 milliseconds, 1L)
      actor ! new Message("channel", Set(), new Event("id2", System.currentTimeMillis ))
      testProbe.expectMsg(200 milliseconds, 2L)
      actor.stop()
    }
    
    "count all events with different categories that occured within a specified time frame" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Nil, new TestPublication(valueDest(testProbe.ref)), true, 500 ), name="test2")
      actor ! new Message("channel", Set("a"), new Event("id1", System.currentTimeMillis))
      testProbe.expectMsg(100 milliseconds, 1L)
      actor ! new Message("channel", Set("b"), new Event("id2", System.currentTimeMillis+1))
      testProbe.expectMsg(100 milliseconds, 1L)
      actor ! new Message("channel", Set("a"), new Event("id3", System.currentTimeMillis+2))
      testProbe.expectMsg(100 milliseconds, 2L)
      actor.stop()
    }
    
    "count different events with the same timestamp" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Subscriptions("channel"), new TestPublication(valueDest(testProbe.ref)), false, 500 ), name="test3")
      val currentTime = System.currentTimeMillis
      actor ! new Message("channel", Set(), new Event("id1", currentTime))
      testProbe.expectMsg(100 milliseconds, 1L)
      actor ! new Message("channel", Set(), new Event("id2", currentTime))      
      testProbe.expectMsg(100 milliseconds, 2L)
      actor.stop()
    }
    
    "stop counter on timeout " in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Subscriptions("channel"), new TestPublication(valueDest(testProbe.ref)), false, 100 ), name="test4")
      actor ! new Message("channel", Set(), new Event("id1", System.currentTimeMillis))
      testProbe.expectMsg(100 milliseconds, 1L)
      Thread.sleep(150)
      testProbe.expectMsg(100 milliseconds, 0L)
      actor.stop()
    }
    
    "timeout if categorize is set to false and no events arrive within the specified timeframe" in {
      val testProbe = TestProbe()
      val actor = TestActorRef(new CountAnalyser(Subscriptions("channel"), new TestPublication(valueDest(testProbe.ref)), false, 10 ), name="test5")
      testProbe.expectMsg(100 milliseconds, 0L)
      actor.stop()
    }
  }
  
  def valueDest(ref: ActorRef) =
    TestActorRef(new Actor {
      def receive = {
        case e: ValueEvent => ref ! e.value
        case _ => fail
      }
    })
}