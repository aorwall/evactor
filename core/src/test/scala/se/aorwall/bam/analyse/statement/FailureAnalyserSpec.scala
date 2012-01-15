package se.aorwall.bam.analyse.statement

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import akka.util.duration.longToDurationLong
import se.aorwall.bam.analyse.statement.window.TimeWindow
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Alert
import se.aorwall.bam.model.State
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FailureAnalyserSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with BeforeAndAfterAll with MustMatchers {

   def this() = this(ActorSystem("FailureAnalyserSpec"))

   val eventName = "event"
   val correlationid = "correlationid"

   override protected def afterAll(): scala.Unit = {
     _system.shutdown()
   }

  "A FailureAnalyser" must {

    "alert when the number of failed incoming events exceeds max allowed failures" in {

     val failureActor = TestActorRef(new FailureAnalyser(eventName, 2))
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! new LogEvent(eventName, correlationid, 0L, "329380921309", "client", "server", State.SUCCESS, "hello") 
     failureActor ! new LogEvent(eventName, correlationid, 1L, "329380921309", "client", "server", State.FAILURE, "hello")
     failureActor ! new LogEvent(eventName, correlationid, 2L, "329380921309", "client", "server", State.FAILURE, "hello")
     probe.expectNoMsg // nothing happens
     failureActor ! new LogEvent(eventName, correlationid, 3L, "329380921309", "client", "server", State.FAILURE, "hello") //  trig alert!

     probe.expectMsg(100 millis, new Alert(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true))
     failureActor.stop()
   }

   "alert when the number of failed incoming events exceeds the max latency within a specified time window" in {

     val time = 100L
     val currentTime = System.currentTimeMillis()

     val failureActor = TestActorRef(new FailureAnalyser(eventName, 2) with TimeWindow {override val timeframe = time} )
     val probe = TestProbe()
     failureActor ! probe.ref

     failureActor ! new LogEvent(eventName, correlationid, currentTime-50, "329380921309", "client", "server", State.FAILURE, "hello")
     failureActor ! new LogEvent(eventName, correlationid, currentTime-40, "329380921309", "client", "server", State.FAILURE, "hello")
     failureActor ! new LogEvent(eventName, correlationid, currentTime-1000, "329380921309", "client", "server", State.FAILURE, "hello") // to old, nothing happens
     failureActor ! new LogEvent(eventName, correlationid, currentTime-30, "329380921309", "client", "server", State.FAILURE, "hello")
     probe.expectMsg(time*2 millis, new Alert(eventName, "3 failed events with name " + eventName + " is more than allowed (2)", true))

     failureActor.stop
   }

   } 
}