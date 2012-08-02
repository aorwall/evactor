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
import org.evactor.EvactorSpec
import org.evactor.subscribe.Subscription
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import akka.util.duration._
import org.evactor.publish.Publication
import org.evactor.model.Message
import org.evactor.expression.Expression
import org.evactor.expression.StaticExpression
import org.evactor.publish.TestPublication

@RunWith(classOf[JUnitRunner])
class ExtractorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec 
  with BeforeAndAfterAll 
  with BeforeAndAfter{

  def this() = this(ActorSystem("ExtractorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  val expectedEvent = new Event("foo", 0L)
  
  class TestExtractor (override val subscriptions: List[Subscription],
        override val publication: Publication,
        override val expression: Expression) extends Extractor(subscriptions, publication, expression) with EventCreator {
         def createBean(value: Option[Any], event: Event with HasMessage) = Some(expectedEvent)
      } 
  
  "An Extractor" must {

    "extract stuff from an events message and send to collector " in {
      val eventPrope = TestProbe()

      val actor = TestActorRef(new TestExtractor(Nil, new TestPublication(eventPrope.ref), new StaticExpression("expr")))
      val event = createDataEvent("stuff")
      
      actor !  new Message("", event)
      
      eventPrope.expectMsg(1 seconds, expectedEvent)
      actor.stop      
    }
    
    "abort if event doesn't extend the HasMessage trait " in {
      val eventPrope = TestProbe()

      val actor = TestActorRef(new TestExtractor(Nil, new TestPublication(eventPrope.ref), new StaticExpression("expr")))
      val event = new Event("id", 0L)
      
      actor ! new Message("", new Event("id", 0L))
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop
    }

  }  
  
}