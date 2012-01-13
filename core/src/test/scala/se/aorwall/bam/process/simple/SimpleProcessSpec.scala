package se.aorwall.bam.process.simple

import grizzled.slf4j.Logging
import se.aorwall.bam.model.{State, Log}
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SimpleProcessSpec extends WordSpec with MustMatchers with Logging {

  val startCompId = "startComponent"
  val endCompId = "endComponent"

  val startComp = new Component(startCompId, 1)
  val endComp = new Component(endCompId, 2)
  val process = new SimpleProcess("process", List(startComp, endComp), 0L)

  "A SimpleProcess" must {

    "return true when it contains a component" in {
      process.handlesEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, "")) must be === true
      process.handlesEvent(new Log("server", endCompId, "corrId", "client", 0L, State.START, "")) must be === true
    }

    "return false when doesn't contain a component" in {
      process.handlesEvent(new Log("server", "anotherComponent", "corrId", "client", 0L, State.START, "")) must be === false
    }


  }

  "A SimpleActivityBuilder" must {

    "create an activity with state SUCCESS when flow is succesfully processed" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.SUCCESS, ""))
      activityBuilder.isFinished must be === false
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.SUCCESS, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.SUCCESS)
    }

    "create an activity with state SUCCESS when flow with just one component is succesfully processed" in {
      val oneComponentProcess = new SimpleProcess("process", List(startComp), 0L)
      val activityBuilder = oneComponentProcess.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.SUCCESS, ""))
      activityBuilder.isFinished must be === true
    }

    "create an activity with state INTERNAL_FAILURE when flow with just one component receives a log event with the state INTERNAL_FAILURE" in {
      val oneComponentProcess = new SimpleProcess("process", List(startComp), 0L)
      val activityBuilder = oneComponentProcess.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.INTERNAL_FAILURE, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.INTERNAL_FAILURE)
    }

    "create an activity with state INTERNAL_FAILURE when a log event has the state INTERNAL_FAILURE" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.INTERNAL_FAILURE, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.INTERNAL_FAILURE)
    }

    "create an activity with state BACKEND_FAILURE when received log events with BACKEND_FAILURE" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", startCompId, "corrId", "client", 0L, State.SUCCESS, ""))
      activityBuilder.isFinished must be === false
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.BACKEND_FAILURE, ""))
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.BACKEND_FAILURE, ""))
      activityBuilder.isFinished must be === false
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", endCompId, "corrId", "client", 0L, State.BACKEND_FAILURE, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.BACKEND_FAILURE)
    }
  }
}