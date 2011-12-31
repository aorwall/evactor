package se.aorwall.logserver.analyse.statement

import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestActorRef, TestKit}

class AbsenceOfRequestsAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers  {

  def this() = this(ActorSystem("AbsenceOfRequestsAnalyserSpec"))

  val process = "process"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsg(300 millis, new Alert(process, "No activities within the timeframe 100ms", true))
      actor.stop()

    }

    "alert on timeout from set timeframe plus the time when the latest activity arrived " in {
      val time = 200L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, time))
      val probe = TestProbe()
      actor ! probe.ref

      Thread.sleep(100)

      actor ! new Activity(process, correlationid, 11, 0, 4)

      probe.expectMsg(250 millis, new Alert(process, "No activities within the timeframe 200ms", true))
      actor.stop()
    }

    "alert on timeout, and send \"back to normal\" message when an activity arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(process, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsg(400 millis, new Alert(process, "No activities within the timeframe 100ms", true))

      actor ! new Activity(process, correlationid, 11, 0, 4)
      probe.expectMsg(new Alert(process, "Back to normal", false))

      actor.stop()
    }
  }
}