package se.aorwall.bam.process

import akka.actor.FaultHandlingStrategy._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.DeadLetterActorRef
import akka.actor.EmptyLocalActorRef
import akka.actor.InternalActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event

abstract class Processor extends Actor with Logging {
  type T <: Event
  
  val strategy = OneForOneStrategy({
	  case _: EventCreationException  => Stop
	  case _: Exception          => Restart
  }: Decider, maxNrOfRetries = Some(10), withinTimeRange = Some(60000))

  val processorId: String
  
  def receive = {
    case event: T => sendToRunningProcessor(event)
    case _ => // skip
  }

  def sendToRunningProcessor(event: T) {
    debug(context.self + " about to process event: " + event)

    val eventId = getEventId(event)    
    debug(context.self + " looking for active event processor with id: " + eventId)
    val runningActivity = getProcessorActor(eventId)
    
    runningActivity ! event    
  }

  def getEventId(event: T): String
  
  def createProcessorActor(id: String): ProcessorActor
  
  def getProcessorActor(eventId: String): ActorRef = context.actorFor(eventId) match {
    case empty: EmptyLocalActorRef => context.actorOf(Props(createProcessorActor(eventId)), eventId)
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