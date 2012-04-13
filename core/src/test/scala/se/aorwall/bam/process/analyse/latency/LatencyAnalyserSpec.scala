package se.aorwall.bam.process.analyse.latency

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.Actor._
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Success
import se.aorwall.bam.process.analyse.window.LengthWindow
import se.aorwall.bam.BamSpec

@RunWith(classOf[JUnitRunner])
class LatencyAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with BamSpec
  with BeforeAndAfterAll{

  def this() = this(ActorSystem("LatencyAnalyserSpec"))

  val name = "name"
  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(Nil, "channel", None, 5))
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! createRequestEvent(0L, None, None, Success, 4) // avg latency 4ms
      latencyActor ! createRequestEvent(1L, None, None, Success, 5)  // avg latency 4.5ms
      probe.expectNoMsg
      latencyActor ! createRequestEvent(3L, None, None, Success, 9) // avg latency 6ms, trig alert!

//      probe.expectMsg(200 millis, new Alert(eventName, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(Nil, "channel", None, 60) with LengthWindow {
        override val noOfRequests = 2
      })
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! createRequestEvent(1L, None, None, Success, 10) // avg latency 10ms
      latencyActor ! createRequestEvent(2L, None, None, Success, 110) // avg latency 55ms
      latencyActor ! createRequestEvent(3L, None, None, Success, 40) // avg latency 75ms, trig alert!

//      probe.expectMsg(100 millis, new Alert(eventName, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor ! createRequestEvent(4L, None, None, Success, 60) // avg latency 55ms, back to normal!

 //     probe.expectMsg(100 millis, new Alert(eventName, "back to normal!", false))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor.stop
    }

  } 
}