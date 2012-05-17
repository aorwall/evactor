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

import org.evactor.bus.ProcessorEventBusExtension
import org.evactor.model.events.LogEvent
import org.evactor._
import org.evactor.subscribe.Subscription
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import org.evactor.model.Message

/**
 * Testing the whole log data flow.
 *
 */
object EvactorIntegrationSuite {
  val conf = ConfigFactory.parseString("""
    evactor {
      
      collectors {
        startCollector {
          publication = { channel = "startComponent" }
        }
      
        endCollector {
          publication = { channel = "endComponent" }
        }
      }
      
      processors {
      
        startComponent {
          type = requestBuilder
          subscriptions = [ {channel = "startComponent"} ]
          publication = { channel = "request" } 
          timeout = 1 minute
        },
      
        endComponent {
          type = requestBuilder
          subscriptions = [ {channel = "endComponent"} ]
          publication = { channel = "request" }
          timeout = 1 minute
        },
      
        simpleProcess {
          type = simpleProcessBuilder
          subscriptions = [ {channel = "request"} ]
          publication = { channel = "process" }
          components = ["startComponent", "endComponent"]
          timeout = 2 minutes
        },
      
        latency {
          type = averageAnalyser
          subscriptions = [ {channel = "process"} ]
          publication = { channel = "latency" } 
          categorize = false
          expression = { mvel = "latency" }
          window = { length = 2 }
        },
      
        alerter {
          type = alerter
          subscriptions = [ {channel = "latency"} ]
          publication = { channel = "latency:alert" }
          categorize = false
          expression = { mvel = "value >= 2000" }
        }
      
      } 
    }
      
    akka {
      # Set loglevel to DEBUG to log everything
      loglevel = WARNING
    }
      
    """)
}

@RunWith(classOf[JUnitRunner])
class EvactorIntegrationSuite(_system: ActorSystem) 
	extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll {
  
  def this() = this(ActorSystem("EvactorIntegrationSuite", EvactorIntegrationSuite.conf))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Recieve log events and send an alert") {    
    
    val probe = TestProbe()
    
    val context = TestActorRef[EvactorContext]("evactor")
    
    ProcessorEventBusExtension(system).subscribe(probe.ref, new Subscription(Some("latency"), None))
    
    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(100)
    
    val startCollector = system.actorFor("%s/collect/startCollector".format(context.path))
    val endCollector = system.actorFor("%s/collect/endCollector".format(context.path)) 
    
    if(startCollector.isTerminated || endCollector.isTerminated) fail ("couldn't find the collectors" )
    
    startCollector ! new LogEvent("329380921309", currentTime, "329380921309", "startComponent", "client", "server", model.Start, "hello")
    startCollector ! new LogEvent("329380921310", currentTime+1000, "329380921309", "startComponent", "client", "server" , model.Success, "") // success
    endCollector ! new LogEvent("329380921311", currentTime+2000, "329380921309", "endComponent", "client", "server", model.Start, "")
    endCollector ! new LogEvent("329380921312",  currentTime+3000, "329380921309", "endComponent", "client", "server", model.Success, "") // success

    probe.expectMsgAllClassOf(1 seconds, classOf[Message]) // the latency alert
  }
}