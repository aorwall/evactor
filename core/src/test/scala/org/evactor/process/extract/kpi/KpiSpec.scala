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
package org.evactor.process.extract.kpi

import org.evactor.model.events.DataEvent
import org.evactor.model.events.KpiEvent
import org.evactor.model.Message
import org.evactor.publish.TestPublication
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import scala.concurrent.duration._
import org.evactor.expression.MvelExpression

@RunWith(classOf[JUnitRunner])
class KpiSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec {

  def this() = this(ActorSystem("KpiSpec"))

  val event = new Message("channel", createDataEvent("{ \"doubleField\": \"123.42\", \"intField\": \"123\", \"anotherField\": \"anothervalue\"}"))

  "Kpi" must {

    "extract float from json messages" in {
      (pending)	// Not supported atm	
    }
  
    "extract int (as float from json messages" in {
      val probe = TestProbe()

      val dest = TestActorRef(new Actor {
        def receive = {
          case e: KpiEvent => probe.ref ! e.value
          case _ => fail
        }
      })

      val kpi = TestActorRef(new KpiExtractor(Nil, new TestPublication(dest), new MvelExpression("message.intField")))
      kpi ! event
      probe.expectMsg(200 millis, 123L)
      kpi.stop
    }

    "send None when a non-numeric value is provided" in {
      val probe = TestProbe()
      val kpi = TestActorRef(new KpiExtractor(Nil, new TestPublication(probe.ref), new MvelExpression("message.anotherField")))
      kpi ! event
      probe.expectNoMsg(200 millis)
      kpi.stop			
    }
  }
}