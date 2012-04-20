package se.aorwall.bam.expression

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestKit
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.BamSpec
import akka.testkit.TestActorRef

@RunWith(classOf[JUnitRunner])
class XPathExpressionEvaluatorSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec {
  
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