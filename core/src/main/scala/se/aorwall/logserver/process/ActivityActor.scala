package se.aorwall.logserver.process

import grizzled.slf4j.Logging
import se.aorwall.logserver.storage.Storing
import akka.actor.{ActorRef, Actor}
import se.aorwall.logserver.model.process.{ActivityBuilder}
import se.aorwall.logserver.model.{Log, Activity}

/**
 * One Activity Actor for each running activity
 */
class ActivityActor(val activityBuilder: ActivityBuilder, val analyser: ActorRef) extends Actor with Storing with Logging{

  // TODO: Check if there already are log events stored in db for this activity : storage.readLogEvents(businessProcess.processId, startEvent.correlationId)

  def receive = {
      case logevent: Log => process(logevent)
  }

  def process(logevent: Log): Unit = {

     debug("Received log event with state: " + logevent.state )

     activityBuilder.addLogEvent(logevent)

     if(activityBuilder.isFinished()){
       debug("Finished: " + logevent)
       sendActivity(activityBuilder.createActivity())
     }
  }

  def sendActivity(activity: Activity): Unit = {

    debug("sending activity: " + activity)

    // Save activity in db
    //TODO storage.storeActivity(activity)

    analyser ! activity

    // stop actor
    self.stop
  }

  // TODO: Timer that stops on timeout and calls createLogevent with state "timeout"
}