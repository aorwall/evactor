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
package org.evactor.process.analyse.window

import scala.collection.immutable.SortedMap
import org.evactor.process.TestProcessor
import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.testkit.TestActorRef
import scala.collection.immutable.TreeMap
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class LengthWindowSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec   
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("LengthWindowSpec"))

  private class TestActor (val probe: ActorRef)
    extends Actor with LengthWindow {
    type S = Int
    val noOfRequests = 3
    
    def receive = {
      case l: SortedMap[Long, Int] => probe ! getInactive(l)
    }
  }
    
  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A LengthWindow" must {

    "return inactive events if there are more events than the specified length" in {
      val probe = TestProbe()
      val actor = TestActorRef(new TestActor(probe.ref))
      actor ! TreeMap(1L -> 11, 2L -> 22, 3L -> 33, 4L -> 44, 5L -> 55)
      probe.expectMsg(100 milliseconds, Map(1L -> 11, 2L -> 22))
    }

    "don't return any events if there are less events than the specified length" in {
      val probe = TestProbe()
      val actor = TestActorRef(new TestActor(probe.ref))
      actor ! TreeMap(1L -> 11, 2L -> 22)
      probe.expectMsg(100 milliseconds, Map())
    }
  }
}