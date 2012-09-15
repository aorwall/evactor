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
package org.evactor.process.analyse.trend

import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.evactor.EvactorSpec
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestActorRef
import org.evactor.publish.TestPublication
import akka.testkit.TestProbe
import org.evactor.model.events.Event
import scala.concurrent.util.duration._
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.ValueEvent

@RunWith(classOf[JUnitRunner])
class RegressionAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("RegressionAnalyserSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }


  "A RegressionAnalyser" must {

    "alert when the regression coefficient overrides a specified value" in {
      (pending)
      
      val testProbe = TestProbe()
      val actor = TestActorRef(new RegressionSubAnalyser(new TestPublication(testProbe.ref), Set("id"), 10, 300 ))
      
      for(i <- 1 until 100){
        actor ! new Event{ val id = "id"; val timestamp = System.currentTimeMillis }
        Thread.sleep(300/i)
      }
      
      testProbe.expectMsgAllClassOf(1 seconds, classOf[ValueEvent])
    }
    
  }
}
