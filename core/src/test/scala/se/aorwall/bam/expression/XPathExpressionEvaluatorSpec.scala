package se.aorwall.bam.expression

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import se.aorwall.bam.model.events.DataEvent
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class XPathExpressionEvaluatorSpec  extends WordSpec with MustMatchers {
  
   "A XPathExpressionEvaluator" must {
     
     "evaluate xpath expressions and return strings" in {
       val evaluator = new XPathExpressionEvaluator("//test")
       val event = new DataEvent("name", "id", 0L, "<test>foo</test>")
       evaluator.execute(event) must be (Some("foo"))
     }
     
     "not evaluate empty xpath expressions" in {
       val evaluator = new XPathExpressionEvaluator("//fail")
       val event = new DataEvent("name", "id", 0L, "<test>foo</test>")
       evaluator.execute(event) must be (None)
     }
     
     "not evaluate invalid xml" in {
       val evaluator = new XPathExpressionEvaluator("//fail")
       val event = new DataEvent("name", "id", 0L, "<test>foo")
       evaluator.execute(event) must be (None)
     }
     
   }

}