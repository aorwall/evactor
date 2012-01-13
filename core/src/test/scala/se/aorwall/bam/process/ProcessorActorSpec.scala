package se.aorwall.bam.process

import org.scalatest.matchers.MustMatchers
import org.mockito.Mockito._
import akka.util.duration._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, CallingThreadDispatcher, TestKit, TestActorRef}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.RequestEvent

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

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.START, "hello")

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

      val logEvent = new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.START, "hello")
      val activity = new RequestEvent("startComponent", "329380921309", 0L, None, None, State.SUCCESS, 0L)

      when(eventBuilder.isFinished).thenReturn(true)
      when(eventBuilder.createEvent).thenReturn(activity)

      actor ! logEvent
      verify(eventBuilder).addEvent(logEvent)

      activityPrope.expectMsg(1 seconds, activity) // The activity returned by activityBuilder should be sent to activityPrope
      actor.stop
    }

    /*
    "send an activity with status TIMEOUT to analyser when timed out" in {
      val timedoutActivity = new RequestEvent("startComponent", "329380921309", 0L, None, None, State.TIMEOUT, 0L)

      val process = new DynamicComponent(1L)

      val timeoutActivityActor = TestActorRef(new ActivityActor("329380921309", process))
      val activityPrope = TestProbe()
      timeoutActivityActor ! activityPrope.ref

      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.START, "hello")

      timeoutActivityActor ! logEvent

      activityPrope.expectMsg(2 seconds, timedoutActivity)

      timeoutActivityActor.stop
    }
    */
  }
}