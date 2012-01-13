package se.aorwall.bam.analyse

import grizzled.slf4j.Logging
import se.aorwall.bam.model.statement.Statement
import collection.mutable.HashMap
import collection.mutable.Map
import akka.actor.TypedActor.{PostStop, PreStart}
import akka.actor.Props._
import se.aorwall.bam.process.ActivityActor
import se.aorwall.bam.model.process.BusinessProcess
import akka.actor.{ActorRef, EmptyLocalActorRef, Props, TypedActor}

/**
 * The reason to have both the AnalyserHandler and the ProcessAnalyser class is
 * to get the path analyse/processId/statementId. TODO: Find a better solution
 */
trait AnalyserHandler {
  def createProcessAnalyser(process: BusinessProcess) // TODO: Return ActorRef?
  def removeProcessAnalyser(processId: String)
  def addStatementToProcess(statement: Statement)
  def removeStatementFromProcess(processId: String, statementId: String)
}

class AnalyserHandlerImpl(val name: String) extends AnalyserHandler with PreStart with PostStop with Logging {

  def this() = this("analyse")

  def createProcessAnalyser(process: BusinessProcess) {
     TypedActor.context.actorOf(Props[ProcessAnalyser], name = process.processId)
  }

  def removeProcessAnalyser(processId: String) {
    val processAnalyser = TypedActor.context.actorFor(processId)
    TypedActor.context.stop(processAnalyser)
  }

  def addStatementToProcess(statement: Statement) {
    debug(TypedActor.context.self + " adding statement " + statement)
    val processAnalyser = TypedActor.context.actorFor(statement.processId) match {
      case empty: EmptyLocalActorRef => TypedActor.context.actorOf(Props[ProcessAnalyser], name = statement.processId)
      case actor: ActorRef => actor
    }
    processAnalyser ! statement
  }

  def removeStatementFromProcess(processId: String, statementId: String) {
    val processAnalyser = TypedActor.context.actorFor(processId)
    processAnalyser ! statementId
  }

  override def preStart(): Unit = {
    debug("Starting analyser handler")
  }

  override def postStop(): Unit = {
    debug("Stopping analyser handler")
//    TypedActor.context.children.foreach { child => TypedActor.context.stop(child) }
  }

}