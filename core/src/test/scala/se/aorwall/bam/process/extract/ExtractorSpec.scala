package se.aorwall.bam.process.extract

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestKit
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.BamSpec
import se.aorwall.bam.process.Subscription
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import akka.util.duration._


@RunWith(classOf[JUnitRunner])
class ExtractorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec 
  with BeforeAndAfterAll 
  with BeforeAndAfter{

  def this() = this(ActorSystem("ExtractorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  class TestExtractor (override val subscriptions: List[Subscription],
        override val channel: String,
        override val expression: String ) extends Extractor(subscriptions, channel, expression) with EventCreator {
         def createBean(value: Option[Any], event: Event, newChannel: String) = Some(event)
      } 
  
  val event = createDataEvent("stuff")
	   
  "An Extractor" must {

    "extract stuff from an events message and send to collector " in {
             
      val actor = TestActorRef(new TestExtractor(Nil, "channel", "expr"))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
      actor ! event
      
      eventPrope.expectMsg(1 seconds, event)
      actor.stop      
    }
    
    "abort if event doesn't extend the HasMessage trait " in {
             
      val actor = TestActorRef(new TestExtractor(Nil, "channel", "expr"))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = createEvent()
	  
      actor ! event 
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop      
    }

  }  
  
}