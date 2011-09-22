package se.aorwall.logserver.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Actor}
import Actor._
import se.aorwall.logserver.storage.Storing
import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.{Log}

class ProcessActor(businessProcess: BusinessProcess, analyserPool: ActorRef) extends Actor with Storing with Logging {

  val runningActivites = new HashMap[String, ActorRef]

  def receive = {
    case logEvent: Log if(businessProcess.contains(logEvent.componentId)) => sendToRunningActivity(logEvent)
    case _ =>
  }

  def sendToRunningActivity(logevent: Log) = {
    debug("About to process logEvent object: " + logevent)

    val id = businessProcess.getActivityId(logevent)

    //TODO storage.storeLogEvent(id, logevent)

    val actors = Actor.registry.actorsFor(id)

    if (actors.length == 0) {

      if (!businessProcess.startNewActivity(logevent)) {
        warn("Didn't receive the start component of the activity: " + logevent)
        // TOOD: Check if a activity is already finished in another actor to not block other requests
      }

      // TODO: Load old activites  storage.getLogEvent(businessProcess.processId, logevent)?
      val actor = actorOf(new ActivityActor(businessProcess.getActivityBuilder(), analyserPool))
      actor.id = id
      actor.start
      actor ! logevent
    } else if (actors.length > 0) {
      actors(0) ! logevent
    } else {
      warn("Didn't handle: " + logevent)
    }
  }
}