package se.aorwall.bam.process.extract.kpi
import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.model.events.Event
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.BamSpec
import se.aorwall.bam.expression.MvelExpression

@RunWith(classOf[JUnitRunner])
class KpiSpec extends BamSpec {

	val event = createDataEvent("{ \"doubleField\": \"123.42\", \"intField\": \"123\", \"anotherField\": \"anothervalue\"}");

	/* TODO: CHanged implementation, must fix tests
	"Kpi" must {

		"extract float from json messages" in {
			val kpi = new Kpi("name", Nil, "channel", MvelExpression("message.doubleField"))
			val kpiEvent = kpi.extract(event)

			kpiEvent match {
				case Some(e: KpiEvent) => e.value must be (123.42)
				case e => fail("expected an instance of se.aorwall.bam.model.events.KpiEvent but found: " + e)
			}		
			
		}
				
		"extract int (as float from json messages" in {
		   val kpi = new Kpi("name", Nil, "channel",  "message.intField")
			val kpiEvent = kpi.extract(event)

			kpiEvent match {
				case Some(e: KpiEvent) => e.value must be (123)
				case e => fail("expected an instance of se.aorwall.bam.model.events.KpiEvent but found: " + e)
			}					
		}
				
		"send None when a non-numeric value is provided" in {
		   val kpi = new Kpi("name", Nil, "channel", "message.anotherField")
			val kpiEvent = kpi.extract(event)

			kpiEvent match {
				case None => 
				case e => fail("expected None but found: " + e)
			}					
		}
		
	}
	*/

}