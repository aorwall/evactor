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
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import akka.util.duration._
import akka.actor.Actor

@RunWith(classOf[JUnitRunner])
class KeywordSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec {

  def this() = this(ActorSystem("KeywordSpec"))

	val event = new Event("channel", None, "id", 0L) with HasMessage {
		val message = "{ \"field\": \"field2\", \"field2\": \"anothervalue\"}"
	}

	val keyword = new Keyword("name", Nil, "channel", new MvelExpression("message.field2"))
	
	"Keyword" must {

		"extract keywords from json messages" in {
		  
			val actor = TestActorRef(keyword.processor)
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      actor ! probe1.ref
      
      actor ! event
            
      val dest = TestActorRef(new Actor {
				def receive = {
					case e: Event => probe2.ref ! e.category
					case _ => fail
				}
			})
			
      probe1.expectMsgAllClassOf(200 millis, classOf[Event])
      probe1.forward(dest)
      probe2.expectMsg(200 millis, Some("anothervalue"))
      actor.stop
		}
	}


}