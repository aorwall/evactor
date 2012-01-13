package se.aorwall.bam.process

import grizzled.slf4j.Logging
import se.aorwall.bam.storage.{LogStorage}
import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Timeout

/**
 * One Activity Actor for each running activity
 */
class ProcessorActor(id: String, eventBuilder: EventBuilder) extends Actor with Logging {

  var storage: Option[LogStorage] = None//TODO FIX

  var testAnalyser: Option[ActorRef] = None // Used for test

  override def preStart() {
//    if (isEventAlreadyFinished(id)){
//      info(context.self + " an activity already exists, aborting...")
//      context.stop(self)
//    } else { 
      trace(context.self + " starting...")
 //   }
  }
  
  override def postRestart(reason: Throwable) {
  
     // TODO: in case of a restart, read active events from db (CF: activeEvent/name/id/****
     //storedEvents.foreach(event => eventBuilder.addEvent(event))
    
      if(eventBuilder.isFinished){
         sendEvent(eventBuilder.createEvent())
      }
  }

  def receive = {
    case event: Event => process(event)
    case Timeout() => sendEvent(eventBuilder.createEvent())
    case actor: ActorRef => testAnalyser = Some(actor)
    case msg => info(context.self + " can't handle: " + msg)
  }

  def process(event: Event) {

      //TODO: Save event to in activeEvents
    
     debug(context.self + " received event : " + event )

     eventBuilder.addEvent(event)

     if(eventBuilder.isFinished){
       debug(context.self + " Finished!")
       sendEvent(eventBuilder.createEvent())
     }
  }

  def sendEvent(event: Event) {

	// TODO: Save event to DB
    
    // send the created event back to collector
    val processor = context.actorFor("/user/collect")
    processor ! event

    // If a test actor exists
    testAnalyser match {
      case Some(testActor) => testActor ! event
      case _ =>
    }

    context.stop(self)
  }

  def eventExists(activityId: String) = false
  // TODO: Check if a finished event already exists
  
  
  override def postStop() {
    trace(context.self + " stopping...")
    eventBuilder.clear()
  }

  def preRestart(reason: Throwable) {  //TODO ???
    warn(context.self + " will be restarted because of an exception", reason)
    eventBuilder.clear()
    preStart()
  }
}

