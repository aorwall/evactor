package se.aorwall.logserver.monitor.statement

import window.LengthWindow
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.util.duration._
import org.scalatest.{WordSpec}
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}

class LatencyAnalyserSpec extends WordSpec with MustMatchers with TestKit {

  val process = "process"
  val correlationid = "correlationid"

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {

      val latencyActor = TestActorRef(new LatencyAnalyser(process, testActor, 5))
      latencyActor.start

      latencyActor ! new Activity(process, correlationid, 10, 0, 4) // avg latency 4ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 15) // avg latency 4.5ms
      latencyActor ! new Activity(process, correlationid, 11, 20, 100) // nothing happens
      //expectNoMsg
      latencyActor ! new Activity(process, correlationid, 10, 30, 39) // avg latency 6ms, trig alert!
      expectMsg(new Alert(process, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {

      val latencyActor = TestActorRef(new LatencyAnalyser(process, testActor, 60) with LengthWindow {
        override val noOfRequests = 2
      })
      latencyActor.start

      latencyActor ! new Activity(process, correlationid, 10, 0, 10) // avg latency 10ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 110) // avg latency 55ms
      latencyActor ! new Activity(process, correlationid, 10, 20, 70) // avg latency 75ms, trig alert!
      expectMsg(new Alert(process, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))

      latencyActor ! new Activity(process, correlationid, 10, 30, 90) // avg latency 55ms, back to normal!
      expectMsg(new Alert(process, "back to normal!", false))

      latencyActor.stop
    }
  }
}