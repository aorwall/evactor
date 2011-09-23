package se.aorwall.logserver.process

import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Actor}
import se.aorwall.logserver.model.process.{ActivityBuilder}
import se.aorwall.logserver.model.{Log, Activity}
import se.aorwall.logserver.storage.{LogStorage, Storing}

/**
 * One Activity Actor for each running activity
 */
class ActivityActor(activityBuilder: ActivityBuilder, storage: LogStorage, analyser: ActorRef) extends Actor with Storing with Logging{

  def receive = {
      case logevent: Log => process(logevent)
  }

  def process(logevent: Log): Unit = {

     debug("Received log event with state: " + logevent.state )

     activityBuilder.addLogEvent(logevent)

     if(activityBuilder.isFinished())
       sendActivity(activityBuilder.createActivity())
  }

  def sendActivity(activity: Activity): Unit = {

    debug("sending activity: " + activity)

    // Save activity in db
    storage.storeActivity(activity)

    analyser ! activity

    // stop actor
    self.stop
  }

  // TODO: Timer that stops on timeout and calls createLogevent with state "timeout"
}