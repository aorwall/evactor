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
package org.evactor

import org.evactor.collect.Collector
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.LogEvent
import org.evactor.model._
import org.evactor.process.analyse.latency.Latency
import org.evactor.process.analyse.window.LengthWindowConf
import org.evactor.process.build.request.Request
import org.evactor.process.build.simpleprocess.SimpleProcess
import org.evactor.process.ProcessorManager
import org.evactor.subscribe.Subscription
import org.evactor.subscribe.Subscriptions
import org.evactor.storage.EventStorageSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.testkit.CallingThreadDispatcher
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import org.evactor.bus.ProcessorEventBusExtension
import org.evactor.publish.StaticPublication
import org.evactor.publish.TestPublication

/**
 * Testing the whole log data flow.
 *
 */
@RunWith(classOf[JUnitRunner])
class EvactorIntegrationSuite(_system: ActorSystem) 
	extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll {
  
  def this() = this(ActorSystem("EvactorIntegrationSuite", EventStorageSpec.storageConf))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Recieve log events and send an alert") {    
    
  	val probe = TestProbe()
  	 
    var result: AlertEvent = null
    val processId = "processId"
    val camelEndpoint = "hej"

    // Start up the modules
    val startCollector = system.actorOf(Props(new Collector(None, None, new StaticPublication("startComponent", Set()))).withDispatcher(CallingThreadDispatcher.Id), name = "request")
    val endCollector = system.actorOf(Props(new Collector(None, None, new StaticPublication("endComponent", Set()))).withDispatcher(CallingThreadDispatcher.Id), name = "endCollector")

    val processor = system.actorOf(Props[ProcessorManager].withDispatcher(CallingThreadDispatcher.Id), name = "process")
          
    // start the processors
    val reqSubscriptions = Subscriptions("request")  

    processor ! new Request("startComponent", List(new Subscription(Some("startComponent"), None)), new StaticPublication("request", Set()), 120000L)
  	processor ! new Request("endComponent", List(new Subscription(Some("endComponent"), None)), new StaticPublication("request", Set()), 120000L)
    processor ! new SimpleProcess("simpleProcess", reqSubscriptions, new StaticPublication(processId, Set()), List("startComponent", "endComponent"), 120000l)  
    processor ! new Latency("latency", List(new Subscription(Some(processId), None)), new TestPublication(probe.ref), 2000, Some(new LengthWindowConf(2)))

    ProcessorEventBusExtension(system).subscribe(probe.ref, new Subscription(Some("latency"), None))
        
    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(400)

    startCollector ! new LogEvent("329380921309", currentTime, "329380921309", "startComponent", "client", "server", Start, "hello")
    startCollector ! new LogEvent("329380921310", currentTime+1000, "329380921309", "startComponent", "client", "server" , Success, "") // success
    endCollector ! new LogEvent("329380921311", currentTime+2000, "329380921309", "endComponent", "client", "server", Start, "")
    endCollector ! new LogEvent("329380921312",  currentTime+3000, "329380921309", "endComponent", "client", "server", Success, "") // success

    Thread.sleep(400)
    
    probe.expectMsgAllClassOf(1 seconds, classOf[AlertEvent]) // the latency alert
  }
}