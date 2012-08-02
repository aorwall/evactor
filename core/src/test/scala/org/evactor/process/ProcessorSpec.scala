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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.testkit.TestKit
import akka.util.duration.intToDurationInt
import org.evactor.model.events.DataEvent
import org.evactor.EvactorSpec
import org.evactor.model.events.Event
import org.evactor.model.Message
import org.evactor.publish.Publication
import org.evactor.subscribe.Subscription
import org.evactor.publish.TestPublication
import org.evactor.publish.Publisher
import com.typesafe.config.ConfigFactory
import org.evactor.process.route.Filter
import org.evactor.process.route.Forwarder
import org.evactor.process.analyse.count.CountAnalyser
import org.evactor.process.analyse.trend.RegressionAnalyser
import org.evactor.process.analyse.window.TimeWindow
import org.evactor.process.analyse.average.AverageAnalyser
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.process.alert.Alerter
import org.evactor.process.build.request.RequestBuilder
import org.evactor.process.build.simpleprocess.SimpleProcessBuilder
import org.evactor.process.produce.LogProducer

class TestProcessor (
    override val subscriptions: List[Subscription],
    val publication: Publication) 
  extends Processor (subscriptions) with Publisher {
  
  override protected def process(event: Event) {
    publish(event)
  }
}

@RunWith(classOf[JUnitRunner])
class ProcessorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec   
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("ProcessorSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  "A Processor" must {

    "process valid event types" in {
      val testProbe = TestProbe()

      val processor = TestActorRef(new TestProcessor(Nil, new TestPublication(testProbe.ref)))
      
      val testEvent = createDataEvent("")
      
      processor ! new Message("", testEvent)
      
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "process valid event types with the right name" in {
      val testEvent = createDataEvent("")
      val testProbe = TestProbe()
      val processor = TestActorRef(new TestProcessor(Nil, new TestPublication(testProbe.ref)))
      
      processor ! new Message("", testEvent)
      
      testProbe.expectMsg(1 seconds, testEvent)
    }
    
    "build filter" in {
      val filterConfig = ConfigFactory.parseString("""
          type = filter
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" } 
          expression = { mvel = "false" }
          accept = false
        """)
        
      TestActorRef(Processor(filterConfig)).underlyingActor match {
        case f: Filter => f.accept must be (false)
        case _ => fail
      }
    }
    
    "build forwarder" in {
      val forwarderConfig = ConfigFactory.parseString("""
          type = forwarder
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" }
        """)
      TestActorRef(Processor(forwarderConfig)).underlyingActor match {
        case f: Forwarder => 
        case _ => fail
      }
    }
    
    "build count analyser" in {
      val countAnalyserConfig = ConfigFactory.parseString("""
          type = countAnalyser
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" } 
          categorization = { NoCategorization {} }
          timeframe = 2 hours
        """)
        
      TestActorRef(Processor(countAnalyserConfig)).underlyingActor match {
        case c: CountAnalyser => {
          c.categorization must be (NoCategorization())
          c.timeframe must be (2*3600*1000L)
        }
        case _ => fail
      }
    }
    
    "build regression analyser" in {
      val regressionAnalyserConfig = ConfigFactory.parseString("""
          type = regressionAnalyser
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" } 
          categorization = { NoCategorization {} }
          minSize = 25
          timeframe = 15 minutes
        """)
      TestActorRef(Processor(regressionAnalyserConfig)).underlyingActor match {
        case r: RegressionAnalyser => {
          r.categorization must be (NoCategorization())
          r.minSize must be (25)
          r.timeframe must be (15*60*1000L)
        }
        case _ => fail
      }
    }
    
    "build average analyser" in {
      val averageAnalyserConfig = ConfigFactory.parseString("""
          type = averageAnalyser
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" }
          categorization = { NoCategorization {} }
          expression = { static = "foo" }
          window = { time = 1 minute }
        """)
        
      val actor =  TestActorRef(Processor(averageAnalyserConfig)).underlyingActor 
      
      actor match {
        case l: AverageAnalyser => l.categorization must be (NoCategorization())
        case _ => fail
      }
    }
    
    "build alerter" in {
      val alerterConfig = ConfigFactory.parseString("""
          type = alerter
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" } 
          categorization = { NoCategorization {} }
          expression = { mvel = "true" }
        """)
      TestActorRef(Processor(alerterConfig)).underlyingActor match {
        case a: Alerter => {
          a.categorization must be (NoCategorization())
        }
        case _ => fail
      }
    }
    
    "build request builder" in {
      val requestBuilderConfig = ConfigFactory.parseString("""
          type = requestBuilder
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" } 
          timeout = 1 minute
        """)
      TestActorRef(Processor(requestBuilderConfig)).underlyingActor match {
        case r: RequestBuilder => {
          r._timeout must be (60000L)
        }
        case _ => fail
      }
    }
    
    "build simple process builder" in {
      val simpleProcessBuilderConfig = ConfigFactory.parseString("""
          type = simpleProcessBuilder
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" }
          components = ["one", "two"]
          timeout = 1 minute
        """)
      TestActorRef(Processor(simpleProcessBuilderConfig)).underlyingActor match {
        case s: SimpleProcessBuilder => {
          s._components must be (List("one", "two"))
          s._timeout must be (60000L)
        }
        case _ => fail
      }
    }
    
    "build log producer" in {
      val logProducerConfig = ConfigFactory.parseString("""
          type = logProducer
          subscriptions = [ {channel = "foo"} ]
          loglevel = DEBUG
        """)
      TestActorRef(Processor(logProducerConfig)).underlyingActor match {
        case l: LogProducer => l.loglevel == "DEBUG"
        case _ => fail
      }
    }
    
    "build custom processor" in {
      val customConfig = ConfigFactory.parseString("""
          class = org.evactor.process.route.Forwarder
          subscriptions = [ {channel = "foo"} ]
          publication = { channel = "bar" }
        """)
      TestActorRef(Processor(customConfig)).underlyingActor match {
        case f: Forwarder => 
        case _ => fail
      }
    }

  }
}