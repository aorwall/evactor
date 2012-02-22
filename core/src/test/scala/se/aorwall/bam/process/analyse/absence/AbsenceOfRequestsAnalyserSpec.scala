package se.aorwall.bam.process.analyse.absence

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.model.events.Event
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AbsenceOfRequestsAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with WordSpec 
  with BeforeAndAfterAll 
  with MustMatchers {

  def this() = this(ActorSystem("AbsenceOfRequestsAnalyserSpec"))

  val name = "event"
  val eventName = Some(classOf[Event].getSimpleName + "/eventName")

  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A AbsenceOfRequestsAnalyser" must {

    "alert on timeout" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(name, eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }
    
    "alert on timeout from set timeframe plus the time when the latest event arrived " in {
      val time = 200L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(name, eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      Thread.sleep(100)

      actor ! new Event(eventName.get, "329380921309", 0L)

      probe.expectMsgAllClassOf(1 second, classOf[AlertEvent])
      actor.stop()
    }

    "alert on timeout, and send \"back to normal\" message when an event arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(name, eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      actor ! new Event("eventName", "329380921309", System.currentTimeMillis)
      probe.expectMsgAllClassOf(1 second, classOf[AlertEvent]) // TODO: Need to check back to normal!

      actor.stop()
    }
        
    "alert on timeout, and don't send \"back to normal\" message when a event with another name arrives" in {
      val time = 100L

      val actor = TestActorRef(new AbsenceOfRequestsAnalyser(name, eventName, time))
      val probe = TestProbe()
      actor ! probe.ref

      probe.expectMsgAllClassOf(1 second, classOf[AlertEvent])

      actor ! new Event("anotherEvent", "329380921309", 0L)
      probe.expectNoMsg(1 second)

      actor.stop()
    }
    
  }

}