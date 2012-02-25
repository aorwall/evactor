package se.aorwall.bam.process.build

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
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.model.events.Event

trait TestEventBuilder extends EventBuilder  {  

  def addEvent(event: Event) {}

  def isFinished = true

  def createEvent() = Right(new Event("name", "id", 0L))

  def clear() {}
    
}

@RunWith(classOf[JUnitRunner])
class BuildActorSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with WordSpec
  with BeforeAndAfterAll
  with MustMatchers
  with BeforeAndAfter {

  def this() = this(ActorSystem("BuildActorSpec"))  
  
  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  "A BuildActor" must {

    "add incoming log events to request list " in {
   	
      var added = false
      
      val actor = TestActorRef(new BuildActor("329380921309", 10000) 
      		with TestEventBuilder { 
      				override def isFinished = false 
      				override def addEvent(event: Event) = added = true
      				def timeout = None
      		})

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")

      actor ! logEvent
      added must be (true)
      actor.stop
    }

    "send the activity to analyser when it's finished " in {
      val probe = TestProbe()
      
      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")
      val reqEvent = new RequestEvent("startComponent", "329380921309", 0L, None, None, Success, 0L)
      
      val actor = TestActorRef(new BuildActor("correlationId", 1000)
      		with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(reqEvent)
      				def timeout = None
      		})

      actor ! probe.ref
      actor ! logEvent	     

      probe.expectMsg(1 seconds, reqEvent) // The activity returned by activityBuilder should be sent to activityPrope
      
      println(actor.isTerminated)
      
      actor.stop
    }

    "send an activity with status TIMEOUT to analyser when timed out" in {
      val probe = TestProbe()

      val timedoutEvent = new RequestEvent("startComponent", "329380921309", 0L, None, None, Timeout, 0L)

      val timeoutEventActor = TestActorRef(new BuildActor("329380921309", 100) with TestEventBuilder { 
      				override def isFinished = true 
      				override def createEvent = Right(timedoutEvent)
      		})

      
      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")
      timeoutEventActor ! probe.ref
      timeoutEventActor ! logEvent

      probe.expectMsg(1 seconds, timedoutEvent)

      timeoutEventActor.stop
    }
  }
}