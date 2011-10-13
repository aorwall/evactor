package se.aorwall.logserver.monitor.statement

import akka.util.duration._
import akka.actor.Actor._
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}
import akka.testkit.{CallingThreadDispatcher, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class AbsenceOfRequestsAnalyserSpec extends WordSpec with BeforeAndAfterAll with MustMatchers with TestKit {

  val process = "process"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    stopTestActor
  }

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {
      val time = 100L

      val actor = actorOf(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.dispatcher = CallingThreadDispatcher.global
      actor.start()

      within(300 millis) {
        expectMsg(new Alert(process, "No activities within the timeframe 100ms", true))
      }
      actor.stop()

    }

    "alert on timeout from set timeframe plus the time when the latest activity arrived " in {
      val time = 200L

      val actor = actorOf(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.dispatcher = CallingThreadDispatcher.global
      actor.start()

      Thread.sleep(100)

      actor ! new Activity(process, correlationid, 11, 0, 4)

      within(250 millis) {
        expectMsg(new Alert(process, "No activities within the timeframe 200ms", true))
      }
      actor.stop()
    }

    "alert on timeout, and send \"back to normal\" message when an activity arrives" in {
      val time = 100L

      val actor = actorOf(new AbsenceOfRequestsAnalyser(process, testActor, time))
      actor.dispatcher = CallingThreadDispatcher.global
      actor.start()

      within(400 millis) {
        expectMsg(new Alert(process, "No activities within the timeframe 100ms", true))
      }

      actor ! new Activity(process, correlationid, 11, 0, 4)
      expectMsg(new Alert(process, "Back to normal", false))

      actor.stop()
      stopTestActor
    }

  }
}