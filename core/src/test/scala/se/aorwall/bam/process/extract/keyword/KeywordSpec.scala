package se.aorwall.bam.process.extract.keyword
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import grizzled.slf4j.Logging
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.expression.MvelExpression

@RunWith(classOf[JUnitRunner])
class KeywordSpec extends WordSpec with MustMatchers with Logging{

	val event = new Event("eventName", "id", 0L) with HasMessage {
		val message = "{ \"field\": \"field2\", \"field2\": \"anothervalue\"}"
	}

	val keyword = new Keyword("keyword", None, new MvelExpression("message.field2"))

	"Keyword" must {

		"extract keywords from json messages" in {
			val newEvent = keyword.extract(event)

			newEvent match {
				case Some(e: Event) => e.name must be ("eventName/keyword/anothervalue")
				case e => fail("expected an instance of se.aorwall.bam.model.events.Event but found: " + e)
			}		
			
		}
	}

}