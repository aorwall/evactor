package se.aorwall.bam.process.simple

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.testkit.TestActorRef
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.Success
import se.aorwall.bam.model.Failure
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll

@RunWith(classOf[JUnitRunner])
class SimpleProcessSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with MustMatchers with Logging {

  def this() = this(ActorSystem("SimpleProcessSpec"))
  
  
  val processId = "process"
  val startCompId = "startComponent"
  val endCompId = "endComponent"

  val actor = TestActorRef(new SimpleProcessProcessor(processId, List("startComponent", "endComponent"), 120000L))
  val processor = actor.underlyingActor

  "A SimpleProcess" must {

    "return true when it contains a component" in {
      processor.handlesEvent(new RequestEvent(startCompId, "329380921309", 0L, None, None, Success, 0L)) must be === true
      processor.handlesEvent(new RequestEvent(endCompId, "329380921309", 0L, None, None, Success, 0L)) must be === true
    }

    "return false when doesn't contain a component" in {
      processor.handlesEvent(new RequestEvent("anotherComponent", "329380921309", 0L, None, None, Success, 0L)) must be === false
    }


  }

  "A SimpleActivityBuilder" must {

    "create an event with state SUCCESS when flow is succesfully processed" in {
      val eventBuilder = new SimpleProcessEventBuilder(processId, List(startCompId, endCompId))	
      eventBuilder.addEvent(new RequestEvent(startCompId, "329380921309", 0L, None, None, Success, 0L))
      eventBuilder.isFinished must be === false
      eventBuilder.addEvent(new RequestEvent(endCompId, "329380921309", 0L, None, None, Success, 0L))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case r: SimpleProcessEvent => r.state must be(Success)
        case _ => fail()
      }  
    }

    "create an activity with state FAILURE when a log event has the state FAILURE" in {
      val eventBuilder = new SimpleProcessEventBuilder(processId, List(startCompId, endCompId))
      eventBuilder.addEvent(new RequestEvent(startCompId, "329380921309", 0L, None, None, Failure, 0L))
      eventBuilder.isFinished must be === true

       eventBuilder.createEvent() match {
        case r: SimpleProcessEvent => r.state must be(Failure)
        case _ => fail()
      }  
    }

  }
}