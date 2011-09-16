package se.aorwall.logserver.process.simple

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.{State, Log, LogEvent}
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.process.simple.{SimpleProcess, Component}

class SimpleProcessSpec extends WordSpec with MustMatchers with Logging {

  val startCompId = "startComponent"
  val endCompId = "endComponent"

  val startComp = new Component(startCompId, 1)
  val endComp = new Component(endCompId, 2)
  val process = new SimpleProcess("process", List(startComp, endComp))

  "A SimpleProcess" must {

    "return true when it contains a component" in {
      process.contains(startCompId) must be === true
      process.contains(endCompId) must be === true
    }

    "return false when doesn't contain a component" in {
      process.contains("anotherComponent") must be === false
    }

    "return true if a request to the start component with state START is provided" in {
      process.startNewActivity(new LogEvent("corrId", startCompId, 0L, State.START)) must be === true
    }

    "return false if a request to the start component with another state is provided" in {
      process.startNewActivity(new LogEvent("corrId", startCompId, 0L, State.SUCCESS)) must be === false
    }

    "return false if a request to another component with state START is provided" in {
      process.startNewActivity(new LogEvent("corrId", endCompId, 0L, State.START)) must be === false
    }

  }

  "A SimpleActivityBuilder" must {

    "create an activity with state SUCCESS when flow is succesfully processed" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.SUCCESS))
      activityBuilder.isFinished() must be === false
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.SUCCESS))
      activityBuilder.isFinished() must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.SUCCESS)
    }


    "create an activity with state INTERNAL_FAILURE when a log event has the state INTERNAL_FAILURE" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.INTERNAL_FAILURE))
      activityBuilder.isFinished() must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.INTERNAL_FAILURE)
    }

    "create an activity with state BACKEND_FAILURE when received log events with BACKEND_FAILURE" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", startCompId, 0L, State.SUCCESS))
      activityBuilder.isFinished() must be === false
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.BACKEND_FAILURE))
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.BACKEND_FAILURE))
      activityBuilder.isFinished() must be === false
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.START))
      activityBuilder.addLogEvent(new LogEvent("corrId", endCompId, 0L, State.BACKEND_FAILURE))
      activityBuilder.isFinished() must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.BACKEND_FAILURE)
    }
  }
}