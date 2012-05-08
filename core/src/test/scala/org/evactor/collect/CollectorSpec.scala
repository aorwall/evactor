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
package org.evactor.collect

import org.evactor.process.TestProcessor
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.testkit.TestActorRef
import akka.util.duration.intToDurationInt
import org.evactor.publish.TestPublication
import org.evactor.model.events.Event

@RunWith(classOf[JUnitRunner])
class CollectorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec   
  with BeforeAndAfterAll {
  
  def this() = this(ActorSystem("CollectorSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  "A Collector" must {

    "publish events to the event bus" in {
      val testProbe = TestProbe()
      val collector = TestActorRef(new Collector(None, None, new TestPublication(testProbe.ref)))
      val testEvent = new Event("id", System.currentTimeMillis)
      collector ! testEvent
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "don't publish events that has already been published" in {
      val testProbe = TestProbe()
      val collector = TestActorRef(new Collector(None, None, new TestPublication(testProbe.ref)))
      val testEvent = new Event("id", System.currentTimeMillis)
      collector ! testEvent
      testProbe.expectMsg(1 seconds, testEvent)
      collector ! testEvent
      testProbe.expectNoMsg(1 seconds)
    }
    
    "send old events to db check" in {
      (pending)
    }
    
    "remove old events from memory" in {
      val collector = TestActorRef(new Collector(None, None, new TestPublication(TestProbe().ref))).underlyingActor

      val testEvent = new Event("id", System.currentTimeMillis - collector.timeInMem + 100)

      collector.eventExists(testEvent) must be (false)
      collector.eventExists(testEvent) must be (true)
      Thread.sleep(200)
      collector.eventExists(testEvent) must be (true)
      collector.eventExists(testEvent) must be (false)
      
    }
    
  }
}
