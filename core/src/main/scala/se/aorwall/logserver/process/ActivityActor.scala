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

  override def preStart {
    storage.readLogs(self.id).foreach(log => process(log))
  }

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

    // Save activity in db and send to analyser

    if(storage.activityExists(activity.processId, activity.activityId)){
      warn("An activity for process " + activity.processId + " with id " + activity.activityId + " already exists")
    } else {
      storage.storeActivity(activity)
      analyser ! activity
    }

    // stop actor
    self.stop
  }

  // TODO: Timer that stops on timeout and calls createLogevent with state "timeout"
}