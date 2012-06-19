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
package org.evactor.process.alert

import org.evactor.expression.MvelExpression
import org.evactor.model.events.ValueEvent
import org.evactor.process.analyse.average.AverageAnalyser
import org.evactor.publish.TestPublication
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import akka.actor.ActorRef
import org.evactor.model.events.AlertEvent
import akka.actor.Actor
import org.evactor.subscribe.Subscriptions
import org.evactor.model.Message

@RunWith(classOf[JUnitRunner])
class AlerterSpec (_system: ActorSystem) 
  extends TestKit(_system)
  with EvactorSpec
  with BeforeAndAfterAll{

  def this() = this(ActorSystem("AlerterSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  "An Alerter" must {
    
    "alert when an expression isn't met" in {
      (pending)
      val probe = TestProbe()
      val alerter = TestActorRef(new Alerter(Subscriptions("channel"), new TestPublication(valueDest(probe.ref)), false, new MvelExpression("value > 0")), name="test1")
      alerter ! new Message("channel", Set(), new ValueEvent("id", System.currentTimeMillis, 1))
      probe.expectMsg(300 milliseconds, true)
    }
    
    "handle back to normal" in {
      (pending)
      val probe = TestProbe()
      val alerter = TestActorRef(new Alerter(Subscriptions("channel"), new TestPublication(valueDest(probe.ref)), false, new MvelExpression("value > 0")), name="test2")
      alerter ! new Message("channel", Set(), new ValueEvent("id", System.currentTimeMillis, 1))
      probe.expectMsg(300 milliseconds, true)
      alerter ! new Message("channel", Set(), new ValueEvent("id", System.currentTimeMillis, 0))
      probe.expectMsg(300 milliseconds, false)
    }
    
    
    "alert when an expression isn't met on events with different categories (categorize = true)" in {
      (pending)
      val probe = TestProbe()
      val alerter = TestActorRef(new Alerter(Subscriptions("channel"), new TestPublication(valueDest(probe.ref)), true, new MvelExpression("value > 0")), name="test1")
      alerter ! new Message("channel", Set("1"), new ValueEvent("id", System.currentTimeMillis, 1))
      probe.expectMsg(300 milliseconds, true)
      alerter ! new Message("channel", Set("2"), new ValueEvent("id", System.currentTimeMillis, 1))
      probe.expectMsg(300 milliseconds, true)
      alerter ! new Message("channel", Set("1"), new ValueEvent("id", System.currentTimeMillis, 0))
      probe.expectMsg(300 milliseconds, false)
    }
    
  }

  def valueDest(ref: ActorRef) =
    TestActorRef(new Actor {
      def receive = {
        case e: AlertEvent => ref ! e.triggered
        case _ => fail
      }
  })

}