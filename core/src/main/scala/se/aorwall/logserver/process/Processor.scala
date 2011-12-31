package se.aorwall.logserver.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.storage.LogStorage
import akka.actor.{Props, ActorRef, Actor}
import se.aorwall.logserver.model.{Activity, Log}

class Processor(businessProcess: BusinessProcess) extends Actor with Logging {

  val runningActivities = new HashMap[String, ActorRef] // TODO: Get rid of this

  var storage: Option[LogStorage] = None//TODO FIX

  def receive = {
    case logEvent: Log if(businessProcess.contains(logEvent.componentId)) => sendToRunningActivity(logEvent)
    case finishedActivity: Activity => stopActivity(finishedActivity.activityId)
    case _ =>
  }

  def sendToRunningActivity(logevent: Log) {
    debug("About to process logEvent object: " + logevent)

    val runningActivity = runningActivities.get(logevent.correlationId)

    runningActivity match {
      case Some(actor) => actor ! logevent
      case None => startActivity(logevent)
    }
  }

  def activityExists(processId: String, activityId: String) = storage match {
    case Some(s) => s.activityExists(processId, activityId)
    case None => false // expect that no former activity exists if no storage is set
  }

  def startActivity(logevent: Log) {
     if (businessProcess.startNewActivity(logevent)){
       val actor = context.actorOf(Props(new ActivityActor(logevent.correlationId, businessProcess)), name = logevent.correlationId)
       runningActivities.put(logevent.correlationId, actor)
    } else if(activityExists(businessProcess.processId, logevent.correlationId)) {
       warn("A finished activity with id " + logevent.correlationId + " already exists.")
    }
  }

  def stopActivity(id: String) {
    val removedActivity = runningActivities.remove(id)
    removedActivity match {
      case Some(actor) => context.stop(actor)
      case _ => warn("No activity actor with id " + id + " found.")
    }
  }

  override def preStart = {
    trace("Starting processor["+context.self+"] for " + businessProcess.processId)


    // TODO: Load started activities

  }

  override def postStop = {
    trace("Stopping processor["+context.self+"] for " + businessProcess.processId)

  }
}