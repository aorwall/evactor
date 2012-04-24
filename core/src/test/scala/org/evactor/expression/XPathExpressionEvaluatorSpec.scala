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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.evactor.model.events.DataEvent
import org.evactor.EvactorSpec
import akka.testkit.TestActorRef

@RunWith(classOf[JUnitRunner])
class XPathExpressionEvaluatorSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec {
  
  def this() = this(ActorSystem("MvelExpressionEvaluatorSpec"))
  
  "A XPathExpressionEvaluator" must {
     
    "evaluate xpath expressions and return strings" in {
      val evaluator = TestActorRef( new XPathExpressionEvaluator{ 
        override val expression = "//test" 
        def receive = { case _ => } }).underlyingActor
      val event = createDataEvent("<test>foo</test>")
      evaluator.evaluate(event) must be (Some("foo"))
    }
     
    "not evaluate empty xpath expressions" in {
      val evaluator = TestActorRef( new XPathExpressionEvaluator{ override val expression = "//fail" 
        def receive = { case _ => } }).underlyingActor
      val event = createDataEvent("<test>foo</test>")
      evaluator.evaluate(event) must be (None)
    }
     
    "not evaluate invalid xml" in {
      val evaluator = TestActorRef( new XPathExpressionEvaluator{ override val expression = "//fail" 
        def receive = { case _ => } }).underlyingActor
      val event = createDataEvent("<test>foo")
      evaluator.evaluate(event) must be (None)
    }
     
  }
}