package se.aorwall.bam.process.analyse.failures

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import akka.util.duration.longToDurationLong
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Failure
import se.aorwall.bam.model.Success
import se.aorwall.bam.process.analyse.window.TimeWindow
import se.aorwall.bam.BamSpec

@RunWith(classOf[JUnitRunner])
class FailureAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec 
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("FailureAnalyserSpec"))

  val name = "name"
  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A FailureAnalyser" must {

    "alert when the number of failed incoming events exceeds max allowed failures" in {

     val failureActor = TestActorRef(new FailureAnalyser(Nil, "channel", None, 2))
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! createLogEvent(0L, Success) 
     failureActor ! createLogEvent(1L, Failure)
     failureActor ! createLogEvent(2L, Failure)
     probe.expectNoMsg // nothing happens
     failureActor ! createLogEvent(3L, Failure) //  trig alert!

     //probe.expectMsg(100 millis, new AlertEvent(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true)) TODO FIX!
     probe.expectMsgAllClassOf(400 millis, classOf[AlertEvent])

     failureActor.stop()
   }

   "alert when the number of failed incoming events exceeds the max latency within a specified time window" in {

     val time = 100L
     val currentTime = System.currentTimeMillis()

     val failureActor = TestActorRef(new FailureAnalyser(Nil, "channel", None, 2) with TimeWindow {override val timeframe = time} )
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! createLogEvent(currentTime-50, Failure)
     failureActor ! createLogEvent(currentTime-40, Failure)
     failureActor ! createLogEvent(currentTime-1000, Failure) // to old, nothing happens
     failureActor ! createLogEvent(currentTime-30, Failure)
     //  probe.expectMsg(time*2 millis, new Alert(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true)) TODO FIX!
     probe.expectMsgAllClassOf(400 millis, classOf[AlertEvent])

     failureActor.stop
   }

   } 
}