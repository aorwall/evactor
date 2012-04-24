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
package org.evactor.process.extract.keyword

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitRunner
import org.evactor.model.events.Event
import org.evactor.model.attributes.HasMessage
import org.evactor.model.events.DataEvent
import org.evactor.expression.MvelExpression
import org.evactor.EvactorSpec
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import akka.util.duration._
import akka.actor.Actor

@RunWith(classOf[JUnitRunner])
class KeywordSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec {

  def this() = this(ActorSystem("KeywordSpec"))

	val event = new Event("channel", None, "id", 0L) with HasMessage {
		val message = "{ \"field\": \"field2\", \"field2\": \"anothervalue\"}"
	}

	val keyword = new Keyword("name", Nil, "channel", new MvelExpression("message.field2"))
	
	"Keyword" must {

		"extract keywords from json messages" in {
		  
			val actor = TestActorRef(keyword.processor)
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      actor ! probe1.ref
      
      actor ! event
            
      val dest = TestActorRef(new Actor {
				def receive = {
					case e: Event => probe2.ref ! e.category
					case _ => fail
				}
			})
			
      probe1.expectMsgAllClassOf(200 millis, classOf[Event])
      probe1.forward(dest)
      probe2.expectMsg(200 millis, Some("anothervalue"))
      actor.stop
		}
	}


}