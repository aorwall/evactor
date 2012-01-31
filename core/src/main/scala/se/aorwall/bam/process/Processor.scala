package se.aorwall.bam.process

import akka.actor.FaultHandlingStrategy._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.DeadLetterActorRef
import akka.actor.EmptyLocalActorRef
import akka.actor.InternalActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging

abstract class Processor (val name: String) extends Actor with ActorLogging {
  type T <: Event
  
  protected var testActor: Option[ActorRef] = None // actor used for testing
  
  def receive  = {
    case event: T => if (handlesEvent(event)) process(event) // TODO: case event: T  doesn't work...
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }

  protected def process(event: T)
  
  protected def handlesEvent(event: T): Boolean
  
  override def preStart = {
    log.debug("starting...")        
  }

  override def postStop = {
    log.debug("stopping...")
  }
}

trait CheckEventName extends Processor with Subscriber with ActorLogging {
  val eventName: Option[String]
  
  protected def handlesEvent(event: T) = eventName match {
    case Some(e) => {
      if(e.endsWith("*")) e.substring(0, e.lastIndexOf("*")-1) == event.path.substring(0, event.path.lastIndexOf("/")) //TODO: Regex!
      else e == event.path
    }
    case None => true
  }
  
  override def preStart = {
    log.debug("subscribing to: \"" + eventName.getOrElse("") + "\"")
    subscribe(context.self, eventName.getOrElse(""))
  }
  
  override def postStop = {
    log.debug("unsubscribing to: \"" + eventName.getOrElse("") + "\"")
    unsubscribe(context.self, eventName.getOrElse(""))
  }
  
}