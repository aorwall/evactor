package se.aorwall.bam.process.extract.kpi

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.BamSpec
import se.aorwall.bam.expression.MvelExpression
import akka.util.duration._


@RunWith(classOf[JUnitRunner])
class KpiSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec {

  def this() = this(ActorSystem("KeywordSpec"))

	val event = createDataEvent("{ \"doubleField\": \"123.42\", \"intField\": \"123\", \"anotherField\": \"anothervalue\"}");

	"Kpi" must {

		"extract float from json messages" in {
			val kpi = new Kpi("name", Nil, "channel", MvelExpression("message.doubleField"))			
			val actor = TestActorRef(kpi.processor)
			
			val probe1 = TestProbe()
      val probe2 = TestProbe()
      actor ! probe1.ref
      
      actor ! event
            
      val dest = TestActorRef(new Actor {
				def receive = {
					case e: KpiEvent => probe2.ref ! e.value
					case _ => fail
				}
			})
			
      probe1.expectMsgAllClassOf(200 millis, classOf[KpiEvent])
      probe1.forward(dest)
      probe2.expectMsg(200 millis, 123.42)
      actor.stop			
		}
				
		"extract int (as float from json messages" in {
		  val kpi = new Kpi("name", Nil, "channel",  MvelExpression("message.intField"))
      val actor = TestActorRef(kpi.processor)

			
	    val probe1 = TestProbe()
      val probe2 = TestProbe()
      actor ! probe1.ref
      
      actor ! event
            
      val dest = TestActorRef(new Actor {
				def receive = {
					case e: KpiEvent => probe2.ref ! e.value
					case _ => fail
				}
			})
			
      probe1.expectMsgAllClassOf(200 millis, classOf[KpiEvent])
      probe1.forward(dest)
      probe2.expectMsg(200 millis, 123)
      actor.stop			
								
		}
				
		"send None when a non-numeric value is provided" in {
		  val kpi = new Kpi("name", Nil, "channel", MvelExpression("message.anotherField"))
	    val actor = TestActorRef(kpi.processor)
			
      val probe1 = TestProbe()
      actor ! probe1.ref
      
      actor ! event
            
      probe1.expectNoMsg(200 millis)
      actor.stop			
			
		}
		
	}
	
}