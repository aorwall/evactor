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
package org.evactor.test.remote

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import grizzled.slf4j.Logging
import org.evactor.test.TestKernel.channels
import org.evactor.test.RequestGenerator
import org.evactor.test.TestKernel

@RunWith(classOf[JUnitRunner])
class RemoteTest(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  import TestKernel._
  
  def this() = this(ActorSystem("RemoteTest"))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  /**
   * This test case is just used to send load to a remote akka system. It doesn't verify anything.
   */
  test("Load test") {   
	  
    val hostname = "localhost"
    
	  val noOfRequestsPerChannel: Int = 1
	  val threads = 1
    val timeBetweenProcesses = 500
    val timeBetweenRequests = 150
    
	  val probes = List.fill(threads)(TestProbe())

	  val collector = system.actorFor("akka://test@%s:2552/user/collect".format(hostname))
	  val timer = system.actorFor("akka://test@%s:2552/user/timer".format(hostname))
	  
	  Thread.sleep(500)
	  
    val requestGenerators: List[ActorRef] = 
      for (probe <- probes) yield
        system.actorOf(Props(new RequestGenerator(channels, collector, probe.ref, noOfRequestsPerChannel, timeBetweenProcesses, timeBetweenRequests)))
    	  
	  Thread.sleep(300)
  
	  val start = System.currentTimeMillis
	  
	  val count: Int = channels.size * noOfRequestsPerChannel
	  
	  timer ! count * threads
	  
	  for(requestGen <- requestGenerators) {
	    requestGen ! None  
	  }
	  
	  for(probe <- probes){
	    probe.receiveN(count, 2 hours)  
	  }
	  
	  println("Finished in " + (System.currentTimeMillis - start) + "ms")
	  
	  Thread.sleep(1000)
	  
  }
}