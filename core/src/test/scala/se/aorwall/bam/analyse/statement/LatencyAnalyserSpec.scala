package se.aorwall.bam.analyse.statement

import se.aorwall.bam.analyse.statement.LatencyAnalyser;
import window.LengthWindow
import akka.actor.Actor._
import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import se.aorwall.bam.model.{Alert, Activity}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.{TestProbe, TestActorRef, CallingThreadDispatcher, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LatencyAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers {

  def this() = this(ActorSystem("LatencyAnalyserSpec"))

  val process = "process"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(process, 5))
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! new Activity(process, correlationid, 10, 0, 4) // avg latency 4ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 15) // avg latency 4.5ms
      latencyActor ! new Activity(process, correlationid, 11, 20, 100) // nothing happens
      probe.expectNoMsg
      latencyActor ! new Activity(process, correlationid, 10, 30, 39) // avg latency 6ms, trig alert!

      probe.expectMsg(200 millis, new Alert(process, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {
      val latencyActor = TestActorRef(new LatencyAnalyser(process, 60) with LengthWindow {
        override val noOfRequests = 2
      })
      val probe = TestProbe()
      latencyActor ! probe.ref

      latencyActor ! new Activity(process, correlationid, 10, 0, 10) // avg latency 10ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 110) // avg latency 55ms
      latencyActor ! new Activity(process, correlationid, 10, 20, 70) // avg latency 75ms, trig alert!

      probe.expectMsg(100 millis, new Alert(process, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))

      latencyActor ! new Activity(process, correlationid, 10, 30, 90) // avg latency 55ms, back to normal!

      probe.expectMsg(100 millis, new Alert(process, "back to normal!", false))
      latencyActor.stop
    }

  }
}