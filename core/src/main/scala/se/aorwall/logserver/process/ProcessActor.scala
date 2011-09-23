package se.aorwall.logserver.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Actor}
import Actor._
import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.{Log}
import se.aorwall.logserver.storage.LogStorage

class ProcessActor(businessProcess: BusinessProcess, storage: LogStorage, analyserPool: ActorRef) extends Actor with Logging {

  val runningActivites = new HashMap[String, ActorRef]

  def receive = {
    case logEvent: Log if(businessProcess.contains(logEvent.componentId)) => sendToRunningActivity(logEvent)
    case _ =>
  }

  def sendToRunningActivity(logevent: Log) = {
    debug("About to process logEvent object: " + logevent)

    val id = businessProcess.getActivityId(logevent)

    storage.storeLog(id, logevent)

    val actors = Actor.registry.actorsFor(id)

    if (actors.length == 0) {

      if (!businessProcess.startNewActivity(logevent) && storage.activityExists(businessProcess.processId, id)) {
        warn("A finished activity with id " + id + " already exists.")
      } else {
        val actor = actorOf(new ActivityActor(businessProcess.getActivityBuilder(), storage, analyserPool))
        actor.id = id
        actor.start

        // Read existing logs for activity
        storage.readLogs(id).foreach(log => actor ! log)

        // ...and finally send the new log if the actor is still alive
        if(actor.isRunning)
          actor ! logevent
      }
    } else if (actors.length > 0) {
      actors(0) ! logevent
    } else {
      warn("Didn't handle: " + logevent)
    }
  }
}