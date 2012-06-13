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

import org.evactor.model.events.Event
import org.evactor.publish.TestPublication
import org.evactor.subscribe.Subscription
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import com.typesafe.config.ConfigFactory
import org.evactor.bus.ProcessorEventBusExtension
import org.evactor.model.Message
import akka.actor.ActorRef
import org.evactor.listen.Listener
import org.evactor.ConfigurationException

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
      val collector = TestActorRef(new Collector(None, None, new TestPublication(TestProbe().ref), 100)).underlyingActor

      val time = System.currentTimeMillis // - collector.timeInMem + 50
      
      val testEvent1 = new Event("id1", time)
      val testEvent2 = new Event("id2", time)
      collector.eventExists(testEvent1) must be (false)
      collector.eventExists(testEvent1) must be (true)
      collector.eventExists(testEvent2) must be (false)
      Thread.sleep(200)
      val newTime = System.currentTimeMillis // - collector.timeInMem + 50
      val newEvent1 = new Event("id1", newTime)
      val newEvent2 = new Event("id2", newTime)
      collector.eventExists(newEvent1) must be (false)
      collector.eventExists(newEvent1) must be (true)
      collector.eventExists(newEvent2) must be (false)
      
    }
    
    "create a collector object from config" in {
    
      val collectorConfig = ConfigFactory.parseString("publication = { channel = \"foo\"}")
          
      val probe = TestProbe()
      ProcessorEventBusExtension(system).subscribe(probe.ref, new Subscription(Some("foo"), None))
    
      val collector = TestActorRef(Collector(collectorConfig))    
      
      collector.underlyingActor match {
        case c: Collector =>
        case _ => fail
      }
      
      val event = new Event("id", 0L)
      collector ! event
      probe.expectMsg(new Message("foo", Set(), event))
      
    }
    
  }
}

