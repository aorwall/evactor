package se.aorwall.bam.process.dynamic

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import se.aorwall.bam.model.Log
import se.aorwall.bam.model.State
import grizzled.slf4j.Logging
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DynamicComponentSpec extends WordSpec with MustMatchers with Logging {

  val compId = "startComponent"
  val process = new DynamicComponent(0L)

  "A DynamicComponent" must {

    "should always return true when contains is called " in {
      process.contains(compId) must be === true
    }

    "return true if a request to the start component with state START is provided" in {
      process.startNewActivity(new Log("server", compId, "corrId", "client", 0L, State.START, "")) must be === true
    }

    "return false if a request to the start component with another state is provided" in {
      process.startNewActivity(new Log("server", compId, "corrId", "client", 0L, State.SUCCESS, "")) must be === false
    }

  }

  "A DynamicComponentActivityBuilder" must {

    "create an activity with state SUCCESS when a component is succesfully processed" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", compId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", compId, "corrId", "client", 0L, State.SUCCESS, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.SUCCESS)
    }

    "create an activity with state INTERNAL_FAILURE when the request to the component has state INTERNAL_FAILURE" in {
      val activityBuilder = process.getActivityBuilder()
      activityBuilder.addLogEvent(new Log("server", compId, "corrId", "client", 0L, State.START, ""))
      activityBuilder.addLogEvent(new Log("server", compId, "corrId", "client", 0L, State.INTERNAL_FAILURE, ""))
      activityBuilder.isFinished must be === true

      val activity = activityBuilder.createActivity()
      activity.state must be(State.INTERNAL_FAILURE)
    }

  }

}