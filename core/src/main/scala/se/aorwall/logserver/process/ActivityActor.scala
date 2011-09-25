package se.aorwall.logserver.process

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.process.{ActivityBuilder}
import se.aorwall.logserver.model.{Log, Activity}
import se.aorwall.logserver.storage.{LogStorage}
import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._

/**
 * One Activity Actor for each running activity
 */
class ActivityActor(activityBuilder: ActivityBuilder, storage: LogStorage, analyser: ActorRef, timeout: Long) extends Actor with Logging{

  var scheduledTimeout: Option[ScheduledFuture[AnyRef]] = None

  override def preStart {
    trace("Starting ActivityActor with id " + self.id)
    val storedLogs = storage.readLogs(self.id)
    storedLogs.foreach(log => process(log))

    if(timeout > 0){
       scheduledTimeout = Some(Scheduler.schedule(self, new Timeout, timeout, timeout, TimeUnit.SECONDS))
    }

  }

  def receive = {
    case logevent: Log => process(logevent)
    case Timeout() => sendActivity(activityBuilder.createActivity())
    case msg => info("Can't handle: " + msg)
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

  override def postStop = {
    trace("Stopping ActivityActor with id " + self.id)
    scheduledTimeout match {
      case Some(s) => s.cancel(true)
      case None => debug("No scheduled timeout to stop in ActivityActor with id: " + self.id)
    }
  }
}

case class Timeout() {

}