package se.aorwall.logserver.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.storage.LogStorage
import se.aorwall.logserver.model.{Activity, Log}
import akka.actor.{EmptyLocalActorRef, Props, ActorRef, Actor}

class Processor(businessProcess: BusinessProcess) extends Actor with Logging {

  def receive = {
    case logEvent: Log if(businessProcess.contains(logEvent.componentId)) => sendToRunningActivity(logEvent)
    case _ =>
  }

  def sendToRunningActivity(logevent: Log) {
    debug(context.self + " about to process logEvent object: " + logevent)

    val runningActivity = context.actorFor(logevent.correlationId)

    runningActivity match {
      case empty: EmptyLocalActorRef => startActivity(logevent)
      case actor: ActorRef => actor ! logevent
      case _ => warn(context.self + " couldn't look up " + logevent.correlationId)
    }
  }

  def startActivity(logevent: Log) {
     if (businessProcess.startNewActivity(logevent)){
       val actor = context.actorOf(Props(new ActivityActor(logevent.correlationId, businessProcess)), name = logevent.correlationId)
       actor ! logevent
    }
  }

  override def preStart = {
    trace(context.self+ " starting...")

    // TODO: Load started activities

  }

  override def postStop = {
    trace(context.self+ " stopping...")
  }
}