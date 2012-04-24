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
package org.evactor.process

import akka.testkit.TestKit
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import org.evactor.model.events.DataEvent
import akka.util.duration._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProcessorEventBusSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with WordSpec 
  with BeforeAndAfterAll 
  with MustMatchers 
  with BeforeAndAfter {

  def this() = this(ActorSystem("ProcessorEventBusSpec"))

  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
    
  val bus = ProcessorEventBusExtension(_system)
  
  val subscriptionCategory = new Subscription(Some("DataEvent"), Some("foo"), Some("bar"))
  val subscription = new Subscription(Some("DataEvent"), Some("foo"), None)
  
  val event1 = new DataEvent("foo", Some("bar"), "", 0L, "")
  val event2 = new DataEvent("bar", None, "", 0L, "")
  
  "A ProcessorEventBus" must {
    
    "publish the given event to the subscriber subscribing to a channel and a category" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, subscriptionCategory)
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, subscriptionCategory)
    }

    "publish the given event to the subscriber subscribing to a channel" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, subscription)
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, subscription)
    }
    
    
    /*
    "publish an event with another channel to the subscriber subscribing to the channel but not the subscriber subscribing to another event name " in {      
      val subscriber1 = TestProbe()
      val subscriber2 = TestProbe()
      bus.subscribe(subscriber1.ref, eventpath_one_eventname)
      bus.subscribe(subscriber2.ref, eventpath_all_eventnames)
      bus.publish(event2)
      subscriber2.expectMsg(1 second, event2)
      subscriber1.expectNoMsg(1 second)
      subscriber2.expectNoMsg(1 second)
      bus.unsubscribe(subscriber1.ref, eventpath_one_eventname)
      bus.unsubscribe(subscriber2.ref, eventpath_all_eventnames)
    }*/	  
        
    "publish an event with another event name to the subscriber subscribing to all events" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, new Subscription(None, None, None))
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, new Subscription(None, None, None))      
    }
  }
}