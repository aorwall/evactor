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

import collection.immutable.TreeMap
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Actor
import scala.collection.immutable.SortedMap
import org.evactor.EvactorSpec
import akka.testkit.TestProbe
import akka.testkit.TestActorRef
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class TimeWindowSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec   
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("TimeWindowSpec"))

  private class TestActor (val probe: ActorRef, val timeframe: Long)
    extends Actor with TimeWindow {
    type S = Int
    
    def receive = {
      case l: SortedMap[Long, Int] => probe ! getInactive(l).size
    }
  }

  val currentTime = System.currentTimeMillis
  val events = TreeMap(
      currentTime - 10000000L -> 11, 
      currentTime - 1000000L -> 22, 
      currentTime - 1000L -> 33, 
      currentTime - 2000L -> 44, 
      currentTime - 3000L -> 55)

  "A TimeWindow" must {
/*
    "return timed out events" in {

      new {
        type S = Int
        val timeframe = 10000L
      } with TimeWindow {
        assert(getInactive(events) == Map(currentTime - 10000000L -> 11, currentTime - 1000000L -> 22))
      }
    }
*/
    "not return any events when the timeframe is set to current time" in {
      val probe = TestProbe()
      val actor = TestActorRef(new TestActor(probe.ref,  System.currentTimeMillis))
      actor ! events
      probe.expectMsg(100 milliseconds, 0)

      
    }

    "return all events when the timelimit is set to 0" in {
      val probe = TestProbe()
      val actor = TestActorRef(new TestActor(probe.ref, 0))
      actor ! events
      probe.expectMsg(100 milliseconds, 5)
    }
  }
}