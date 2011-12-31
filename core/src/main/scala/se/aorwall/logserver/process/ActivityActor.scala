package se.aorwall.logserver.process

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.{Log, Activity}
import se.aorwall.logserver.storage.{LogStorage}
import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._
import akka.util.duration._
import se.aorwall.logserver.model.process.BusinessProcess

/**
 * One Activity Actor for each running activity
 */
class ActivityActor(id: String, businessProcess: BusinessProcess) extends Actor with Logging{
  var scheduledTimeout: Option[Cancellable] = None

  val activityBuilder = businessProcess.getActivityBuilder

  var storage: Option[LogStorage] = None//TODO FIX

  var testAnalyser: Option[ActorRef] = None // Used for test

  override def preStart() {
    trace("Starting " + context.self)

    val storedLogs = storage match {
      case Some(s) => s.readLogs(id)
      case None => List[Log]()
    }

    storedLogs.foreach(log => activityBuilder.addLogEvent(log))
    //TODO: Save activity in runningActivitesCF

    if(activityBuilder.isFinished){
       sendActivity(activityBuilder.createActivity())
    } else if (businessProcess.timeout > 0){

      // set timeout to (timeout - the time since the first element)
      val timeoutSinceStart = if(storedLogs.size > 0) businessProcess.timeout - (System.currentTimeMillis - storedLogs.map(_.timestamp).min)
                              else businessProcess.timeout

      if(timeoutSinceStart > 0) {
        debug("Activity actor with id " + id + " will timeout in " + timeoutSinceStart + " seconds")
        scheduledTimeout = Some(context.system.scheduler.scheduleOnce(timeoutSinceStart seconds, self, new Timeout))
      } else {
        warn("Activity has already timed out!")
        sendActivity(activityBuilder.createActivity())
      }
    }
  }

  def receive = {
    case logevent: Log => process(logevent)
    case Timeout() => sendActivity(activityBuilder.createActivity())
    case actor: ActorRef => testAnalyser = Some(actor)
    case msg => info("Can't handle: " + msg)
  }

  def process(logevent: Log) {

     debug("Received log event with state: " + logevent.state )

     activityBuilder.addLogEvent(logevent)

     if(activityBuilder.isFinished){
       debug("Finished: " + logevent)
       sendActivity(activityBuilder.createActivity())
     }
  }

  def sendActivity(activity: Activity) {

    debug("sending activity: " + activity)

    if(activityExists(activity.processId, activity.activityId)){
      warn("An activity for process " + activity.processId + " with id " + activity.activityId + " already exists")
    } else {
      storage match {
        case Some(s) => s.finishActivity(activity)
        case None =>
      }

      val statementAnalysers = context.actorSelection("../../analyser/" + activity.processId)
      statementAnalysers ! activity

      // If a test actor exists
      testAnalyser match {
        case Some(testActor) => testActor ! activity
        case _ =>
      }
    }

    context.become {
        case _ => info("Sent a message to an inactive actvity actor with processid " + businessProcess.processId + " with correlation id " + id)
    }

    context.parent ! activity
  }

  def activityExists(processId: String, activityId: String) = storage match {
    case Some(s) => s.activityExists(processId, activityId)
    case None => false // expect that no former activity exists if no storage is set
  }

  override def postStop() {
    trace("Stopping " + context.self)
    scheduledTimeout match {
      case Some(s) => s.cancel()
      case None => debug("No scheduled timeout to stop in ActivityActor with id: " + id)
    }
    activityBuilder.clear()
  }

  def preRestart(reason: Throwable) {  //TODO ???
    warn("Activiy actor will be restarted because of an exception", reason)
    activityBuilder.clear()
    preStart()
  }
}

case class Timeout() {

}
