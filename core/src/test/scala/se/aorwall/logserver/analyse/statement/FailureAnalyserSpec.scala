package se.aorwall.logserver.analyse.statement

import akka.actor.Actor._
import window.{TimeWindow}
import akka.util.duration._
import org.scalatest.matchers.MustMatchers
import se.aorwall.logserver.model.{Alert, Activity}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.{TestProbe, TestActorRef, CallingThreadDispatcher, TestKit}

class FailureAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers {

   def this() = this(ActorSystem("FailureAnalyserSpec"))

   val process = "process"
   val correlationid = "correlationid"

   override protected def afterAll(): scala.Unit = {
     _system.shutdown()
   }

  "A FailureAnalyser" must {

    "alert when the number of failed incoming activities exceeds max allowed failures" in {

     val failureActor = TestActorRef(new FailureAnalyser(process, List(11,12), 2))
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! new Activity(process, correlationid, 11, 0, 4) //
     failureActor ! new Activity(process, correlationid, 12, 10, 15) //
     failureActor ! new Activity(process, correlationid, 13, 20, 100) // nothing happens
     probe.expectNoMsg
     failureActor ! new Activity(process, correlationid, 11, 30, 39) //  trig alert!

     probe.expectMsg(300 millis, new Alert(process, "3 failed activites in process " + process + " with state List(11, 12) is more than allowed (2)", true))
     failureActor.stop()
   }

   "alert when the number of failed incoming activities exceeds the max latency within a specified time window" in {

     val time = 100L
     val currentTime = System.currentTimeMillis()

     val failureActor = TestActorRef(new FailureAnalyser(process, List(11), 2) with TimeWindow {override val timeframe = time} )
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! new Activity(process, correlationid, 11, currentTime-50, currentTime)
     failureActor ! new Activity(process, correlationid, 11, currentTime-40, currentTime)
     failureActor ! new Activity(process, correlationid, 11, currentTime-1000, currentTime) // to old, nothing happens
     failureActor ! new Activity(process, correlationid, 11, currentTime-30, currentTime)
     probe.expectMsg(time*2 millis, new Alert(process, "3 failed activites in process " + process + " with state List(11) is more than allowed (2)", true))

     failureActor.stop
   }

   }
}