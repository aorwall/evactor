package se.aorwall.bam.process

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import se.aorwall.bam.model.events.Event
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorRef

class TestProcessor (name: String, val eventName: Option[String]) 
  extends Processor (name) 
  with CheckEventName {
  
  type T = TestEvent
    
  override def receive = {
    case event: TestEvent if(handlesEvent(event)) =>  process(event)
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  protected def process(event: TestEvent) {
    testActor match {
      case Some(actor) => actor ! event
      case None => 
    }
  }  
}

case class TestEvent (override val name: String, override val id: String, override val timestamp: Long) extends Event(name, id, timestamp)

case class OtherEvent (override val name: String, override val id: String, override val timestamp: Long) extends Event(name, id, timestamp)

@RunWith(classOf[JUnitRunner])
class ProcessorSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with MustMatchers {

  def this() = this(ActorSystem("ProcessorSpec"))

  
  "A Processor" must {

    "process valid event types" in {
      
      val processor = TestActorRef(new TestProcessor("329380921309", None))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      val testEvent = new TestEvent("name", "id", System.currentTimeMillis)
      
      processor ! testEvent
      
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "process valid event types with the right name" in {
      val testEvent = new TestEvent("name", "id", System.currentTimeMillis)
      
      val processor = TestActorRef(new TestProcessor("329380921309", Some(testEvent.path)))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      processor ! testEvent
      
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "not process invalid event types" in {
      val processor = TestActorRef(new TestProcessor("329380921309", Some("name")))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      val testEvent = new OtherEvent("name", "id", System.currentTimeMillis)
      
      processor ! testEvent
      
      testProbe.expectNoMsg(1 seconds)
    }    
    
    "not process valid event types with invalid name" in {
      val processor = TestActorRef(new TestProcessor("329380921309", Some("name")))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      val testEvent = new TestEvent("otherName", "id", System.currentTimeMillis)
      
      processor ! testEvent
      
      testProbe.expectNoMsg(1 seconds)
    }

  }
}