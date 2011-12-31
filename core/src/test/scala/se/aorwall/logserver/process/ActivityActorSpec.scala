package se.aorwall.logserver.process

import dynamic.DynamicComponent
import org.scalatest.matchers.MustMatchers
import org.mockito.Mockito._
import se.aorwall.logserver.model.process.simple.{SimpleActivityBuilder}
import se.aorwall.logserver.model.{Log, Activity, State}
import se.aorwall.logserver.storage.LogStorage
import akka.util.duration._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import se.aorwall.logserver.model.process.BusinessProcess
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, CallingThreadDispatcher, TestKit, TestActorRef}

class ActivityActorSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers with BeforeAndAfter{

  def this() = this(ActorSystem("ActivityActorSpec"))

  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }

  "A ActivityActor" must {

    "add incoming log events to request list " in {

      val activityBuilder = mock(classOf[SimpleActivityBuilder])
      val businesProcess = mock(classOf[BusinessProcess])
      when(businesProcess.getActivityBuilder).thenReturn(activityBuilder)
      val activityActorRef = TestActorRef(new ActivityActor("329380921309", businesProcess))

      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.START, "hello")

      when(activityBuilder.isFinished()).thenReturn(false)

      activityActorRef ! logEvent
      verify(activityBuilder).addLogEvent(logEvent)
      activityActorRef.stop
    }

    "send the activity to analyser when it's finished " in {
      val activityBuilder = mock(classOf[SimpleActivityBuilder])
      val businesProcess = mock(classOf[BusinessProcess])

      when(businesProcess.getActivityBuilder).thenReturn(activityBuilder)
      val activityActorRef = TestActorRef(new ActivityActor("correlationId", businesProcess))

      val activityPrope = TestProbe()
      activityActorRef ! activityPrope.ref

      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.SUCCESS, "hello")
      val activity = new Activity("processId", "correlationId", State.SUCCESS, 0L, 10L)

      when(activityBuilder.isFinished()).thenReturn(true)
      when(activityBuilder.createActivity()).thenReturn(activity)

      activityActorRef ! logEvent
      verify(activityBuilder).addLogEvent(logEvent)

      activityPrope.expectMsg(1 seconds, activity) // The activity returned by activityBuilder should be sent to activityPrope
      activityActorRef.stop
    }

    "send an activity with status TIMEOUT to analyser when timed out" in {
      val timedoutActivity = new Activity("startComponent", "329380921309", State.TIMEOUT, 0L, 0L)

      val process = new DynamicComponent(1L)

      val timeoutActivityActor = TestActorRef(new ActivityActor("329380921309", process))
      val activityPrope = TestProbe()
      timeoutActivityActor ! activityPrope.ref

      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.START, "hello")

      timeoutActivityActor ! logEvent

      activityPrope.expectMsg(2 seconds, timedoutActivity)

      timeoutActivityActor.stop
    }
  }
}