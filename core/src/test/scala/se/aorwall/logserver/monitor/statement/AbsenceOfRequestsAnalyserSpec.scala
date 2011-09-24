package se.aorwall.logserver.monitor.statement

import akka.testkit.TestActorRef

import akka.testkit.TestKit
import window.{TimeWindow, LengthWindow}
import akka.util.duration._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}

class AbsenceOfRequestsAnalyserSpec extends WordSpec with MustMatchers with TestKit {

  val process = "process"
  val correlationid = "correlationid"

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {

      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.start

      within(time * 2 millis) {
        expectMsg(new Alert(process, "No activities within the timeframe 100ms", true))
      }

    }

    "alert on timeout, and send \"back to normal\" message when an activity arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.start

      within(time * 2 millis) {
        expectMsg(new Alert(process, "No activities within the timeframe 100ms", true))
      }

      actor ! new Activity(process, correlationid, 11, 0, 4)
      expectMsg(new Alert(process, "Back to normal", false))
    }
  }
}