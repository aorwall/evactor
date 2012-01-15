package se.aorwall.bam.analyse.statement

import window.LengthWindow
import akka.actor.Actor._
import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import se.aorwall.bam.model.Alert
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.{TestProbe, TestActorRef, CallingThreadDispatcher, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.State

@RunWith(classOf[JUnitRunner])
class LatencyAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers {

  def this() = this(ActorSystem("LatencyAnalyserSpec"))

  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(eventName, 5))
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! new RequestEvent(eventName, correlationid, 0L, None, None, State.SUCCESS, 4) // avg latency 4ms
      latencyActor ! new RequestEvent(eventName, correlationid, 1L, None, None, State.SUCCESS, 5)  // avg latency 4.5ms
      probe.expectNoMsg
      latencyActor ! new RequestEvent(eventName, correlationid, 3L, None, None, State.SUCCESS, 9) // avg latency 6ms, trig alert!

      probe.expectMsg(200 millis, new Alert(eventName, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(eventName, 60) with LengthWindow {
        override val noOfRequests = 2
      })
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! new RequestEvent(eventName, correlationid, 1L, None, None, State.SUCCESS, 10) // avg latency 10ms
      latencyActor ! new RequestEvent(eventName, correlationid, 2L, None, None, State.SUCCESS, 110) // avg latency 55ms
      latencyActor ! new RequestEvent(eventName, correlationid, 3L, None, None, State.SUCCESS, 40) // avg latency 75ms, trig alert!

      probe.expectMsg(100 millis, new Alert(eventName, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))

      latencyActor ! new RequestEvent(eventName, correlationid, 4L, None, None, State.SUCCESS, 60) // avg latency 55ms, back to normal!

      probe.expectMsg(100 millis, new Alert(eventName, "back to normal!", false))
      latencyActor.stop
    }

  } 
}