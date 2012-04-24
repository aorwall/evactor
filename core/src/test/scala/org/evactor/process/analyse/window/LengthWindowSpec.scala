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
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestActorRef
import akka.actor.Actor
import org.evactor.process.analyse.Analyser

@RunWith(classOf[JUnitRunner])
class LengthWindowSpec extends WordSpec with MustMatchers {

  val lengthWindow = new {
      type S = Int
      val noOfRequests = 3
    } with LengthWindow 

  "A LengthWindowConf" must {

    "return inactive events if there are more events than the specified length" in {
      val events = TreeMap(1L -> 11, 2L -> 22, 3L -> 33, 4L -> 44, 5L -> 55)
      lengthWindow.getInactive(events) must be(Map(1L -> 11, 2L -> 22))
    }

    "don't return any events if there are less events than the specified length" in {
      val events = TreeMap(1L -> 11, 2L -> 22)
      lengthWindow.getInactive(events).size must be(0)
    }

  }
}