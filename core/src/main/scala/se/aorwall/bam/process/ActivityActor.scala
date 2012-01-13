package se.aorwall.bam.process

import grizzled.slf4j.Logging
import se.aorwall.bam.model.{Log, Activity}
import se.aorwall.bam.storage.{LogStorage}
import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._
import akka.util.duration._
import se.aorwall.bam.model.process.BusinessProcess

/**
 * One Activity Actor for each running activity
 */
class ActivityActor(id: String, businessProcess: BusinessProcess) extends Actor with Logging {
  var scheduledTimeout: Option[Cancellable] = None

  val activityBuilder = businessProcess.getActivityBuilder

  var storage: Option[LogStorage] = None//TODO FIX

  var testAnalyser: Option[ActorRef] = None // Used for test

  override def preStart() {
    if (activityExists(businessProcess.processId, id)){
      info(context.self + " an activity already exists, aborting...")
      context.stop(self)
    } else {
      trace(context.self + " starting...")
      scheduledTimeout = Some(context.system.scheduler.scheduleOnce(businessProcess.timeout seconds, self, new Timeout))
    }
  }
  
  override def postRestart(reason: Throwable) {
  
     // in case of a restart, all logs have to be read from storage
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
          debug(context.self + " will timeout in " + timeoutSinceStart + " seconds")
          scheduledTimeout = Some(context.system.scheduler.scheduleOnce(timeoutSinceStart seconds, self, new Timeout))
        } else {
          warn(context.self +  " has already timed out!")
          sendActivity(activityBuilder.createActivity())
        }
      }
  }

  def receive = {
    case logevent: Log => process(logevent)
    case Timeout() => sendActivity(activityBuilder.createActivity())
    case actor: ActorRef => testAnalyser = Some(actor)
    case msg => info(context.self + " can't handle: " + msg)
  }

  def process(logevent: Log) {

     debug(context.self + " received log event with state: " + logevent.state )

     activityBuilder.addLogEvent(logevent)

     if(activityBuilder.isFinished){
       debug("Finished: " + logevent)
       sendActivity(activityBuilder.createActivity())
     }
  }

  def sendActivity(activity: Activity) {

    storage match {
      case Some(s) => s.finishActivity(activity)
      case None =>
    }

    val processAnalyser = context.actorFor("/user/analyse/" + activity.processId)
    debug(context.self + " sending " + activity + " to /user/analyse/" + activity.processId )
    processAnalyser ! activity

    // If a test actor exists
    testAnalyser match {
      case Some(testActor) => testActor ! activity
      case _ =>
    }

    context.stop(self)
  }

  def activityExists(processId: String, activityId: String) = storage match {
    case Some(s) => s.activityExists(processId, activityId)
    case None => false // expect that no former activity exists if no storage is set
  }

  override def postStop() {
    trace(context.self + " stopping...")
    scheduledTimeout match {
      case Some(s) => s.cancel()
      case None => debug(context.self + " no scheduled timeout to stop")
    }
    activityBuilder.clear()
  }

  def preRestart(reason: Throwable) {  //TODO ???
    warn(context.self + " will be restarted because of an exception", reason)
    activityBuilder.clear()
    preStart()
  }
}

case class Timeout() {

}
