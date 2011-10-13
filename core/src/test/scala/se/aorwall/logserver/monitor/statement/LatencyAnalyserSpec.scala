package se.aorwall.logserver.monitor.statement

import window.LengthWindow
import akka.actor.Actor._
import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}
import akka.testkit.{CallingThreadDispatcher, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class LatencyAnalyserSpec extends WordSpec with BeforeAndAfterAll with MustMatchers with TestKit {

  val process = "process"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    stopTestActor
  }

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {
      val latencyActor = actorOf(new LatencyAnalyser(process, testActor, 5))
      latencyActor.dispatcher = CallingThreadDispatcher.global
      latencyActor.start

      latencyActor ! new Activity(process, correlationid, 10, 0, 4) // avg latency 4ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 15) // avg latency 4.5ms
      latencyActor ! new Activity(process, correlationid, 11, 20, 100) // nothing happens
      //expectNoMsg
      latencyActor ! new Activity(process, correlationid, 10, 30, 39) // avg latency 6ms, trig alert!

      within (200 millis){
        expectMsg(new Alert(process, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))
      }

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {
      val latencyActor = actorOf(new LatencyAnalyser(process, testActor, 60) with LengthWindow {
        override val noOfRequests = 2
      })
      latencyActor.dispatcher = CallingThreadDispatcher.global
      latencyActor.start
      latencyActor ! new Activity(process, correlationid, 10, 0, 10) // avg latency 10ms
      latencyActor ! new Activity(process, correlationid, 10, 10, 110) // avg latency 55ms
      latencyActor ! new Activity(process, correlationid, 10, 20, 70) // avg latency 75ms, trig alert!

      within (100 millis){
        expectMsg(new Alert(process, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))
      }

      latencyActor ! new Activity(process, correlationid, 10, 30, 90) // avg latency 55ms, back to normal!

      within (100 millis){
        expectMsg(new Alert(process, "back to normal!", false))
      }
      latencyActor.stop
    }

  }
}