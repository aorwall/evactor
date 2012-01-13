package se.aorwall.bam.process

import collection.mutable.HashMap
import grizzled.slf4j.Logging
import se.aorwall.bam.model.process.BusinessProcess
import se.aorwall.bam.storage.LogStorage
import se.aorwall.bam.model.{Activity, Log}
import akka.actor.{EmptyLocalActorRef, Props, ActorRef, Actor}
import akka.actor.DeadLetterActorRef
import akka.actor.InternalActorRef

class Processor(businessProcess: BusinessProcess) extends Actor with Logging {

  def receive = {
    case logEvent: Log if(businessProcess.handlesEvent(logEvent)) => sendToRunningActivity(logEvent)
    case _ => 
  }

  def sendToRunningActivity(logevent: Log) {
    debug(context.self + " about to process logEvent object: " + logevent)

    val activityId = businessProcess.getActivityId(logevent)    
    debug(context.self + " looking for active actor with id: " + activityId)
    val runningActivity = getActivity(activityId)
    
    runningActivity ! logevent    
  }

  def getActivity(id: String): ActorRef = context.actorFor(id) match {
    case empty: EmptyLocalActorRef => {
      trace(context.self + " found " + empty + " starting actor with id: " + id)
      context.actorOf(Props(new ActivityActor(id, businessProcess)), name = id)
    }
    case actor: ActorRef => {
     trace(context.self + " found: " + actor)
     actor
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