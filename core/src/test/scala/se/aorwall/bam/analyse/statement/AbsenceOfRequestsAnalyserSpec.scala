package se.aorwall.bam.analyse.statement

import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.Alert
import se.aorwall.bam.model.events.Event

@RunWith(classOf[JUnitRunner])
class AbsenceOfRequestsAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers  {

  def this() = this(ActorSystem("AbsenceOfRequestsAnalyserSpec"))

  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsg(200 millis, new Alert(eventName, "No events within the timeframe 100ms", true))
      actor.stop()

    }
    
    "alert on timeout from set timeframe plus the time when the latest event arrived " in {
      val time = 200L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      Thread.sleep(100)

      actor ! new Event(eventName, "329380921309", 0L)

      probe.expectMsg(300 millis, new Alert(eventName, "No events within the timeframe 200ms", true))
      actor.stop()
    }

    "alert on timeout, and send \"back to normal\" message when an event arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsg(200 millis, new Alert(eventName, "No events within the timeframe 100ms", true))

      actor ! new Event(eventName, "329380921309", System.currentTimeMillis)
      probe.expectMsg(new Alert(eventName, "Back to normal", false))

      actor.stop()
    }
    
    
    "alert on timeout, and don't send \"back to normal\" message when a event with another name arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsg(200 millis, new Alert(eventName, "No events within the timeframe 100ms", true))

      actor ! new Event("anotherEvent", "329380921309", 0L)
      probe.expectNoMsg(200 millis)

      actor.stop()
    }
    
  }

}