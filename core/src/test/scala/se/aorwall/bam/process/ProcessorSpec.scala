package se.aorwall.bam.process

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.testkit.TestKit
import akka.util.duration.intToDurationInt
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.BamSpec

class TestProcessor (override val subscriptions: List[Subscription]) 
  extends Processor (subscriptions)  {
  
  type T = DataEvent
    
  override def receive = {
    case event: DataEvent => process(event)
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  protected def process(event: DataEvent) {
    testActor match {
      case Some(actor) => actor ! event
      case None => 
    }
  }  
}

@RunWith(classOf[JUnitRunner])
class ProcessorSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec   
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("ProcessorSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  "A Processor" must {

    "process valid event types" in {
      
      val processor = TestActorRef(new TestProcessor(Nil))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      val testEvent = createDataEvent("")
      
      processor ! testEvent
      
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "process valid event types with the right name" in {
      val testEvent = createDataEvent("")
      
      val processor = TestActorRef(new TestProcessor(Nil))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      processor ! testEvent
      
      testProbe.expectMsg(1 seconds, testEvent)
    }

    "not process invalid event types" in {
      val processor = TestActorRef(new TestProcessor(Nil))
      val testProbe = TestProbe()
      processor ! testProbe.ref
      
      val testEvent = createEvent()
      
      processor ! testEvent
      
      testProbe.expectNoMsg(1 seconds)
    }    

  }
}