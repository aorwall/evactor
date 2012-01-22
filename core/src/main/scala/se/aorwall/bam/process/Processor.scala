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

abstract class Processor (val name: String) extends Actor with Logging {
  type T <: Event
  
  protected val collector = context.actorFor("/user/collect")
  protected var testActor: Option[ActorRef] = None // actor used for testing
  
  def receive  = {
    case event: T => if (handlesEvent(event)) process(event) // TODO: case event: T  doesn't work...
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }

  protected def process(event: T)
  
  protected def handlesEvent(event: T): Boolean
  
  override def preStart = {
    trace(context.self+ " starting...")
  }

  override def postStop = {
    trace(context.self+ " stopping...")
  }
}

trait CheckEventName extends Processor {
  val eventName: Option[String]
  
  protected def handlesEvent(event: T) = eventName match {
    case Some(e) => e == event.name
    case None => true
  }
}