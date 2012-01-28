package se.aorwall.bam.expression

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.DataEvent
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MvelExpressionEvaluatorSpec  extends WordSpec with MustMatchers {
  
   "A MvelExpressionEvaluator" must {
     
     "evaluate and return strings" in {
       val evaluator = new MvelExpressionEvaluator[String]("message.replace('%','&')")
       val event = new DataEvent("name", "id", 0L, "%foo%bar%")
       evaluator.execute(event) must be (Some("&foo&bar&"))
     }

     "evaluate string and return boolean" in {
       val evaluator = new MvelExpressionEvaluator[Boolean]("message == 'foo'")
       val event1 = new DataEvent("name", "id", 0L, "foo")
       evaluator.execute(event1) must be (Some(true))
       val event2 = new DataEvent("name", "id", 0L, "bar")
       evaluator.execute(event2) must be (Some(false))
     }

     "evaluate json and return string" in {
       val evaluator = new MvelExpressionEvaluator[Boolean]("message.foo.bar")
       val event1 = new DataEvent("name", "id", 0L, "{ \"foo\": { \"bar\": \"value\" } }")
       evaluator.execute(event1) must be (Some("value"))
     }
     
     "evaluate date in json and return Int" in {
       val evaluator = new MvelExpressionEvaluator[Boolean]("new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.second).getTime() - new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss\").parse(message.first).getTime()")
       val event1 = new DataEvent("name", "id", 0L, "{ \"first\": \"2012-01-27T15:57:00+01:00\", \"second\": \"2012-01-27T16:01:00+01:00\" }")
       evaluator.execute(event1) must be (Some(4 * 60 * 1000))
     }
   }

}