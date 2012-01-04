package se.aorwall.logserver.process

import se.aorwall.logserver.model.process.BusinessProcess
import collection.mutable.HashMap
import grizzled.slf4j.Logging
import akka.actor.{Props, ActorRef, TypedActor}
import akka.actor.TypedActor.{PreStart, PostStop}

trait ProcessorHandler {
  def setProcess(process: BusinessProcess): Unit
  def removeProcess(processId: String): Unit
}

class ProcessorHandlerImpl(val name: String) extends ProcessorHandler with PreStart with PostStop with Logging  {

  def this() = this("processor")

  val activeProcesses = new HashMap[String, ActorRef]

  def setProcess(process: BusinessProcess) {
    // TODO: Must handle incoming log events when process actor is inactive. Maybe by putting log receivers on hold?
    val currentProcess = activeProcesses.get(process.processId)
    stopProcess(currentProcess)

    debug("Starting processor for process: " + process.processId + " in context " + TypedActor.context.self)
    val newProcess = TypedActor.context.actorOf(Props(new Processor(process)), name = process.processId)
    activeProcesses.put(process.processId, newProcess)
  }

  def removeProcess(processId: String) {
    debug("Stopping processor for process: " + processId)
     val currentProcess = activeProcesses.remove(processId)
     stopProcess(currentProcess)
  }

  def stopProcess(process: Option[ActorRef]) {
    process match {
      case Some(actor) => TypedActor.context.stop(actor)
      case None => debug("No process actor to stop")
    }
  }

  override def preStart(): Unit = {
    debug("Starting processor handler")
  }

  override def postStop(): Unit = {
    debug("Stopping processor handler")
    //TypedActor.context.children.foreach { child => TypedActor.context.stop(child) }
  }
}
