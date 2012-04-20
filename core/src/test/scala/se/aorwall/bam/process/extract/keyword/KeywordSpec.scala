package se.aorwall.bam.process.extract.keyword
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.expression.MvelExpression
import se.aorwall.bam.BamSpec

@RunWith(classOf[JUnitRunner])
class KeywordSpec extends BamSpec {

	val event = new Event("channel", None, "id", 0L) with HasMessage {
		val message = "{ \"field\": \"field2\", \"field2\": \"anothervalue\"}"
	}

	val keyword = new Keyword("name", Nil, "channel", new MvelExpression("message.field2"))
	
	/* TODO: Changed implementation, must fix tests
	"Keyword" must {

		"extract keywords from json messages" in {
			val newEvent = keyword.extract(event)

			newEvent match {
				case Some(e: Event) => e.category must be (Some("anothervalue"))
				case e => fail("expected an instance of se.aorwall.bam.model.events.Event but found: " + e)
			}		
			
		}
	}
  */

}