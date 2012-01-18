package se.aorwall.bam.extract

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.model.Timeout
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.events.RequestEvent
import org.mockito.Mockito._
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event

@RunWith(classOf[JUnitRunner])
class ExtractorSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers with BeforeAndAfter{

  def this() = this(ActorSystem("ExtractorSpec"))

  val extractedEvent = new Event("eventName", "id", 0L)
  
  def extract (event: Event with HasMessage): Option[Event] = Some(extractedEvent)   
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  "An Extractor" must {

    "extract stuff from an events message and send to collector " in {
             
      val actor = TestActorRef(new Extractor("eventName", extract))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = new Event("eventName", "id", 0L) with HasMessage {
			val message = "stuff"
	   }
	  
      actor ! event
      
      eventPrope.expectMsg(1 seconds, extractedEvent)
      actor.stop      
    }
    
    "abort if event with wrong name arrives " in {
             
      val actor = TestActorRef(new Extractor("eventName", extract))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = new Event("wrongEvent", "id", 0L) with HasMessage {
			val message = "stuff"
	   }
	  
      actor ! event 
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop      
    }
    
    "abort if event doesn't extend the HasMessage trait " in {
             
      val actor = TestActorRef(new Extractor("eventName", extract))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = new Event("eventName", "id", 0L)
	  
      actor ! event 
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop      
    }

  }  
  
}