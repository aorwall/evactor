package se.aorwall.logserver.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Actor}
import Actor._
import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.{Log}
import se.aorwall.logserver.storage.LogStorage
import akka.config.Supervision.OneForOneStrategy

class ProcessActor(businessProcess: BusinessProcess, storage: Option[LogStorage], analyserPool: ActorRef) extends Actor with Logging {
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 3, 5000)

  val runningActivites = new HashMap[String, ActorRef]

  def receive = {
    case logEvent: Log if(businessProcess.contains(logEvent.componentId)) => sendToRunningActivity(logEvent)
    case _ =>
  }

  def sendToRunningActivity(logevent: Log) = {
    debug("About to process logEvent object: " + logevent)

    val id = businessProcess.getActivityId(logevent)

    storage match {
      case Some(s) => s.storeLog(id, logevent)
      case None =>
    }

    val actors = Actor.registry.actorsFor(id)

    if (actors.length == 0) {

      if (!businessProcess.startNewActivity(logevent) && activityExists(businessProcess.processId, id)) {
        warn("A finished activity with id " + id + " already exists.")
      } else {
        val actor = actorOf(new ActivityActor(businessProcess.getActivityBuilder, storage, analyserPool, businessProcess.timeout))
        actor.id = id
        self.link(actor)
        actor.start

        // ...and finally send the new log if the actor is still alive
        if(actor.isRunning)
          actor ! logevent
        else
          warn("Couldn't send log event to: " + actor)
      }
    } else  {
      actors(0) ! logevent
    }
  }

  def activityExists(processId: String, activityId: String) = storage match {
    case Some(s) => s.activityExists(processId, activityId)
    case None => false // expect that no former activity exists if no storage is set
  }

  override def preStart = {
    trace("Starting processActor for " + businessProcess.processId)
  }

  override def postStop = {
    trace("Stopping processActor for " + businessProcess.processId)
  }
}