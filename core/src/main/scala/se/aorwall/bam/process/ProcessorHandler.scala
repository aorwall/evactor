package se.aorwall.bam.process

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.routing.BroadcastRouter
import se.aorwall.bam.model.events.Event
import akka.util.Index
import akka.actor.ActorLogging
import se.aorwall.bam.storage.StorageProcessor
import se.aorwall.bam.storage.StorageProcessorRouter
import akka.actor.Status

class ProcessorHandler extends Actor with ActorLogging  {
  
  override def preStart = {
    log.debug("starting...")
  }
  
  def receive = {
    case configuration: ProcessorConfiguration => setProcessor(configuration)
    case name: String => removeProcessor(name)
    case msg => log.warning("can't handle: {}", msg)
  }
  
  /**
   * Add and start new processor in the actor context. Will fail if
   * an exception is thrown on startup.
   */
  def setProcessor(configuration: ProcessorConfiguration) {
    try {
	    log.debug("starting processor for configuration: {}", configuration)
	    
      context.actorOf(Props(configuration.processor), name = configuration.name)
      sender ! Status.Success
    } catch {
			case e: Exception => {
			  log.warning("Starting processor with name {} failed. {}", configuration.name, e)
			  sender ! Status.Failure(e)
			}
	  }
  }

  def removeProcessor(name: String) {    
    try {
	    log.debug("stopping processor with name: {}", name)
	    val runningActor = context.actorFor(name)
	    context.stop(runningActor)  
	    sender ! Status.Success    
    } catch {
			case e: Exception => sender ! Status.Failure(e)
	  }
  }

  override def postStop() {
    log.debug("stopping...")
  }
}
