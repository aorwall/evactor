package se.aorwall.logserver.monitor.statement

import akka.actor.Actor._
import window.{TimeWindow}
import akka.util.duration._
import org.scalatest.{WordSpec}
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}
import akka.testkit.{CallingThreadDispatcher, TestKit}

class FailureAnalyserSpec extends WordSpec with MustMatchers with TestKit {

   val process = "process"
   val correlationid = "correlationid"

  "A FailureAnalyser" must {

    "alert when the number of failed incoming activities exceeds max allowed failures" in {

     val failureActor = actorOf(new FailureAnalyser(process, testActor, List(11,12), 2))
     failureActor.dispatcher = CallingThreadDispatcher.global
     failureActor.start

     failureActor ! new Activity(process, correlationid, 11, 0, 4) //
     failureActor ! new Activity(process, correlationid, 12, 10, 15) //
     failureActor ! new Activity(process, correlationid, 13, 20, 100) // nothing happens
     //expectNoMsg
     failureActor ! new Activity(process, correlationid, 11, 30, 39) //  trig alert!

     within (300 millis) {
      expectMsg(new Alert(process, "3 failed activites in process " + process + " with state List(11, 12) is more than allowed (2)", true))
     }
     failureActor.stop
   }

   "alert when the number of failed incoming activities exceeds the max latency within a specified time window" in {

     val time = 100L
     val currentTime = System.currentTimeMillis()

     val failureActor = actorOf(new FailureAnalyser(process, testActor, List(11), 2) with TimeWindow {override val timeframe = time} )
     failureActor.dispatcher = CallingThreadDispatcher.global
     failureActor.start

     within (time*2 millis) {
        failureActor ! new Activity(process, correlationid, 11, currentTime-50, currentTime)
        failureActor ! new Activity(process, correlationid, 11, currentTime-40, currentTime)
        failureActor ! new Activity(process, correlationid, 11, currentTime-1000, currentTime) // to old, nothing happens
        failureActor ! new Activity(process, correlationid, 11, currentTime-30, currentTime)
        expectMsg(new Alert(process, "3 failed activites in process " + process + " with state List(11) is more than allowed (2)", true))
     }

     failureActor.stop
   }

   }
}