package se.aorwall.logserver.process

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.mockito.Mockito._
import akka.testkit.{TestKit, TestActorRef}
import se.aorwall.logserver.model.process.simple.{SimpleActivityBuilder}
import se.aorwall.logserver.model.{Log, Activity, State}

class ActivityActorSpec extends WordSpec with MustMatchers with TestKit {

  "A ActivityActor" must {

    val activityBuilder = mock(classOf[SimpleActivityBuilder])

    val activityActorRef = TestActorRef(new ActivityActor(activityBuilder, testActor))
    activityActorRef.start
    val activityActor = activityActorRef.underlyingActor

    "add incoming log events to request list " in {
      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.START, "hello")

      when(activityBuilder.isFinished()).thenReturn(false)

      activityActorRef ! logEvent
      verify(activityBuilder).addLogEvent(logEvent)
    }

    "send the activity to analyser when it's finished " in {
      val logEvent = new Log("server", "startComponent", "329380921309", "client", 0L, State.SUCCESS, "hello")
      val activity = new Activity("processId", "correlationId", State.SUCCESS, 0L, 10L)

      when(activityBuilder.isFinished()).thenReturn(true)
      when(activityBuilder.createActivity()).thenReturn(activity)

      activityActorRef ! logEvent
      verify(activityBuilder).addLogEvent(logEvent)
      expectMsg(activity) // The activity returned by activityBuilder should be sent to testActor

    }
  }

}