package se.aorwall.bam.process.extract

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
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.BamSpec

@RunWith(classOf[JUnitRunner])
class ExtractorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec 
  with BeforeAndAfterAll 
  with BeforeAndAfter{

  def this() = this(ActorSystem("ExtractorSpec"))

  val extractedEvent = createEvent()
  
  def extract (event: Event with HasMessage): Option[Event] = Some(extractedEvent)   
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  val event = createDataEvent("stuff")
	   
  "An Extractor" must {

    "extract stuff from an events message and send to collector " in {
             
      val actor = TestActorRef(new Extractor(Nil, "channel", extract))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	  
      actor ! event
      
      eventPrope.expectMsg(1 seconds, extractedEvent)
      actor.stop      
    }
    
    "abort if event doesn't extend the HasMessage trait " in {
             
      val actor = TestActorRef(new Extractor(Nil, "channel", extract))
      
      val eventPrope = TestProbe()
      actor ! eventPrope.ref
      
	   val event = createEvent()
	  
      actor ! event 
      
      eventPrope.expectNoMsg(1 seconds)
      actor.stop      
    }

  }  
  
}