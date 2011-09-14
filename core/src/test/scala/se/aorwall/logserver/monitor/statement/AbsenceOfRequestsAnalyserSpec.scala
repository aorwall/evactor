package se.aorwall.logserver.monitor.statement

import akka.testkit.TestActorRef

import se.aorwall.logserver.model.Activity
import akka.testkit.TestKit
import window.{TimeWindow, LengthWindow}
import akka.util.duration._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers

class AbsenceOfRequestsAnalyserSpec extends WordSpec with MustMatchers with TestKit {

  val process = "process"
  val correlationid = "correlationid"

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {

      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.start

      within(time * 2 millis) {
        expectMsg("No activities within the timeframe 100ms")
      }

    }

    "alert on timeout, and send \"back to normal\" message when an activity arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.start

      within(time * 2 millis) {
        expectMsg("No activities within the timeframe 100ms")
      }

      actor ! new Activity(process, correlationid, 11, 0, 4)
      expectMsg("Back to normal")
    }
  }
}