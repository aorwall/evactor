package se.aorwall.bam.process.build.request

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.model.Failure
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import se.aorwall.bam.process.build.BuildActor

@RunWith(classOf[JUnitRunner])
class RequestBuilderSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with WordSpec
  with MustMatchers
  with Logging {

  def this() = this(ActorSystem("RequestBuilderSpec"))
  
  val compId = "startComponent"
  
  val actor = TestActorRef(new RequestBuilder("processor", 0L))
  val processor = actor.underlyingActor

  "A RequestProcessor" must {

    "should always return true when handlesEvent is called " in {
      processor.handlesEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello")) must be === true
    }

  }

  "A RequestEventBuilder" must {

    "create a RequestEvent with state SUCCESS when a component is succesfully processed" in {
      val buildActor = TestActorRef(new BuildActor("329380921309", 1000) 
      		with RequestEventBuilder { 
      			def timeout = Some(1000L)
      		})
      	
      val eventBuilder = buildActor.underlyingActor
      		
      eventBuilder.addEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello"))
      eventBuilder.addEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Success, "hello"))
      eventBuilder.isFinished must be === true

      eventBuilder.createEvent() match {
        case Right(r: RequestEvent) => r.state must be(Success)
        case _ => fail()
      }     
    }

    "create a RequestEvent with state FAILURE when the LogEvent to the component has state FAILURE" in {
       val buildActor = TestActorRef(new BuildActor("329380921309", 1000) 
      		with RequestEventBuilder { 
      			def timeout = Some(1000L)
      		})
      	
      val eventBuilder = buildActor.underlyingActor
      
      eventBuilder.addLogEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Start, "hello"))
      eventBuilder.addLogEvent(new LogEvent("startComponent", "329380921309", 0L, "329380921309", "client", "server", Failure, "hello"))
      eventBuilder.isFinished must be === true
      
      eventBuilder.createEvent() match {
        case Right(r: RequestEvent) => r.state must be(Failure)
        case _ => fail()
      }     
    }

    
  }

}