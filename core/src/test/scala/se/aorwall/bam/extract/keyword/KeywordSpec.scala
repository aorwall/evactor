package se.aorwall.bam.extract.keyword
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import grizzled.slf4j.Logging
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.KeywordEvent

@RunWith(classOf[JUnitRunner])
class KeywordSpec extends WordSpec with MustMatchers with Logging{

	val event = new Event("eventName", "id", 0L) with HasMessage {
		val message = "{ \"field\": \"field2\", \"field2\": \"anothervalue\"}"
	}

	val keyword = new Keyword("eventName", "keyword", "field2")

	"Keyword" must {

		"extract keywords from json messages" in {
			val keywordEvent = keyword.extract(event)

			keywordEvent match {
				case Some(e: KeywordEvent) => e.keyword must be ("anothervalue")
				case e => fail("expected an instance of se.aorwall.bam.model.events.KeywordEvent but found: " + e)
			}		
			
		}
	}

}