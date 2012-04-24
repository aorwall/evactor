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
package org.evactor.expression

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.evactor.model.events.Event
import org.evactor.model.events.DataEvent
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.evactor.BamSpec
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.actor.ActorSystem

@RunWith(classOf[JUnitRunner])
class MvelExpressionEvaluatorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec {
  
  def this() = this(ActorSystem("MvelExpressionEvaluatorSpec"))
  
  "A MvelExpressionEvaluator" must {
     
    "evaluate and return strings" in {
      val evaluator = TestActorRef( new MvelExpressionEvaluator { 
        override val expression = "message.replace('%','&')"
        def receive = { case _ => } }).underlyingActor
      val event = createDataEvent("%foo%bar%")
      evaluator.evaluate(event) must be (Some("&foo&bar&"))
    }
     
    /*

     "evaluate string and return boolean" in {
       val evaluator = new MvelExpressionEvaluator[Boolean]("message == 'foo'")
       val event1 = new DataEvent("name", "id", 0L, "foo")
       evaluator.execute(event1) must be (Some(true))
       val event2 = new DataEvent("name", "id", 0L, "bar")
       evaluator.execute(event2) must be (Some(false))
     }
	 */
     
    "evaluate json and return string" in {
      val evaluator = TestActorRef ( new MvelExpressionEvaluator { override val expression ="message.foo.bar" 
        def receive = { case _ => } }).underlyingActor
      val event1 = createDataEvent("{ \"foo\": { \"bar\": \"value\" } }")
      evaluator.evaluate(event1) must be (Some("value"))
    }
     
    "evaluate date in json and return Int" in {
      val evaluator = TestActorRef ( new MvelExpressionEvaluator { override val expression = "new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.second).getTime() - new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.first).getTime()"
        def receive = { case _ => } }).underlyingActor 
      val event1 = createDataEvent("{ \"first\": \"2012-01-27T15:57:00+01:00\", \"second\": \"2012-01-27T16:01:00+01:00\" }")
      evaluator.evaluate(event1) must be (Some((4 * 60 * 1000).toString))
    }
  }

}