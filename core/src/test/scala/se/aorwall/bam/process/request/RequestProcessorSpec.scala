package se.aorwall.bam.process.request

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.State
import se.aorwall.bam.process.request.RequestEventBuilder
import se.aorwall.bam.process.request.RequestProcessor
import se.aorwall.bam.model.events.RequestEvent
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef

@RunWith(classOf[JUnitRunner])
class RequestProcessorSpec(_system: ActorSystem) extends TestKit(_system) with  WordSpec with MustMatchers with Logging {

  def this() = this(ActorSystem("RequestProcessorSpec"))
  
  val compId = "startComponent"
  
  val actor = TestActorRef(new RequestProcessor("processor", 0L))
  val processor = actor.underlyingActor

  "A RequestProcessor" must {

    "should always return true when handlesEvent is called " in {
      processor.handlesEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.START, "hello")) must be === true
    }

  }

  "A RequestEventBuilder" must {

    "create a RequestEvent with state SUCCESS when a component is succesfully processed" in {
      val eventBuilder = new RequestEventBuilder
      eventBuilder.addEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.START, "hello"))
      eventBuilder.addEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.SUCCESS, "hello"))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case r: RequestEvent => r.state must be(State.SUCCESS)
        case _ => fail()
      }     
    }

    "create a RequestEvent with state FAILURE when the LogEvent to the component has state FAILURE" in {
      val eventBuilder = new RequestEventBuilder
      eventBuilder.addLogEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.START, "hello"))
      eventBuilder.addLogEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", State.FAILURE, "hello"))
      eventBuilder.isFinished must be === true
      
      eventBuilder.createEvent() match {
        case r: RequestEvent => r.state must be(State.FAILURE)
        case _ => fail()
      }     
    }

  }

}