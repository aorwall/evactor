package se.aorwall.bam.process

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

@RunWith(classOf[JUnitRunner])
class ProcessorActorSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers with BeforeAndAfter{

  def this() = this(ActorSystem("ProcessorActorSpec"))

  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
  
  "A ProcessorActor" must {

    "add incoming log events to request list " in {

      val eventBuilder = mock(classOf[EventBuilder])
      val actor = TestActorRef(new ProcessorActor("329380921309", eventBuilder))

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")

      when(eventBuilder.isFinished).thenReturn(false)

      actor ! logEvent
      verify(eventBuilder).addEvent(logEvent)
      actor.stop
    }

    "send the activity to analyser when it's finished " in {
      val eventBuilder = mock(classOf[EventBuilder])
      val actor = TestActorRef(new ProcessorActor("correlationId", eventBuilder))

      val activityPrope = TestProbe()
      actor ! activityPrope.ref

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")
      val activity = new RequestEvent("startComponent", "329380921309", 0L, None, None, Success, 0L)

      when(eventBuilder.isFinished).thenReturn(true)
      when(eventBuilder.createEvent).thenReturn(activity)

      actor ! logEvent	
      verify(eventBuilder).addEvent(logEvent)

      activityPrope.expectMsg(1 seconds, activity) // The activity returned by activityBuilder should be sent to activityPrope
      actor.stop
    }

    "send an activity with status TIMEOUT to analyser when timed out" in {
      val eventBuilder = mock(classOf[EventBuilder])
      val timedoutActivity = new RequestEvent("startComponent", "329380921309", 0L, None, None, Timeout, 0L)

      val timeoutActivityActor = TestActorRef(new ProcessorActor("329380921309", eventBuilder)  with Timed { _timeout = Some(1L)})

      when(eventBuilder.createEvent).thenReturn(timedoutActivity)
      
      val activityPrope = TestProbe()
      timeoutActivityActor ! activityPrope.ref

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")

      timeoutActivityActor ! logEvent

      activityPrope.expectMsg(2 seconds, timedoutActivity)

      timeoutActivityActor.stop
    }
  }
}