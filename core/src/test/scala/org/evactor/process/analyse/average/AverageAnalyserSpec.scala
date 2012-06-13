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
package org.evactor.process.analyse.average

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.Actor._
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.Success
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.EvactorSpec
import org.evactor.model.Message
import org.evactor.publish.TestPublication
import org.evactor.expression.MvelExpression
import akka.actor.Actor
import org.evactor.model.events.ValueEvent
import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory

@RunWith(classOf[JUnitRunner])
class AverageAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with EvactorSpec
  with BeforeAndAfterAll{

  def this() = this(ActorSystem("AverageAnalyserSpec"))

  val name = "name"
  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A AverageAnalyser" must {

    "count the average latency of the incoming request events" in {
      (pending)
      val probe = TestProbe()

      val avg = TestActorRef(new AverageAnalyser(Nil, new TestPublication(valueDest(probe.ref)), false, new MvelExpression("latency"), None))

      avg ! new Message("", Set(), createRequestEvent(0L, None, None, "corr", "comp", Success, 4))
      probe.expectMsg(200 milliseconds, 4.0)
      avg ! new Message("", Set(), createRequestEvent(1L, None, None, "corr", "comp", Success, 5)) 
      probe.expectMsg(200 milliseconds, 4.5)
      avg ! new Message("", Set(), createRequestEvent(3L, None, None, "corr", "comp", Success, 9)) 
      probe.expectMsg(200 milliseconds, 6.0)
      avg.stop
    }

    "count average latency of the incoming requests events within a specified length window" in {
      (pending)
      val probe = TestProbe()
      val winConf = ConfigFactory.parseString("length = 2")
      val avg = TestActorRef(new AverageAnalyser(Nil, new TestPublication(valueDest(probe.ref)), false, new MvelExpression("latency"), Some(winConf)))
      
      avg ! new Message("", Set(), createRequestEvent(1L, None, None, "corr", "comp", Success, 10))
      probe.expectMsg(200 milliseconds, 10.0)
      avg ! new Message("", Set(), createRequestEvent(2L, None, None, "corr", "comp", Success, 110))
      probe.expectMsg(200 milliseconds, 60.0)
      avg ! new Message("", Set(), createRequestEvent(3L, None, None, "corr", "comp", Success, 40))
      probe.expectMsg(200 milliseconds, 75.0)
      avg ! new Message("", Set(), createRequestEvent(4L, None, None, "corr", "comp", Success, 60))
      probe.expectMsg(200 milliseconds, 50.0)

      avg.stop
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