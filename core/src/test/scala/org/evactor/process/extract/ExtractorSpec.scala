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
package org.evactor.process.extract

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.DataEvent
import org.evactor.model.events.Event
import org.evactor.BamSpec
import org.evactor.process.Subscription
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class ExtractorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec 
  with BeforeAndAfterAll 
  with BeforeAndAfter{

  def this() = this(ActorSystem("ExtractorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  class TestExtractor (override val subscriptions: List[Subscription],
        override val channel: String,
        override val expression: String ) extends Extractor(subscriptions, channel, expression) with EventCreator {
         def createBean(value: Option[Any], event: Event, newChannel: String) = Some(event)
      } 
  
  val event = createDataEvent("stuff")
	   
  "An Extractor" must {

    "extract stuff from an events message and send to collector " in {
             
      val actor = TestActorRef(new TestExtractor(Nil, "channel", "expr"))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
      actor ! event
      
      eventPrope.expectMsg(1 seconds, event)
      actor.stop      
    }
    
    "abort if event doesn't extend the HasMessage trait " in {
             
      val actor = TestActorRef(new TestExtractor(Nil, "channel", "expr"))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = createEvent()
	  
      actor ! event 
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop      
    }

  }  
  
}