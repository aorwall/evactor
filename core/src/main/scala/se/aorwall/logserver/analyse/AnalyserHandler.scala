package se.aorwall.logserver.analyse

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.statement.Statement
import collection.mutable.HashMap
import collection.mutable.Map
import akka.actor.{Props, TypedActor, ActorRef}
import akka.actor.TypedActor.{PostStop, PreStart}

trait AnalyserHandler {
  def addStatementToProcess(statement: Statement)
  def removeStatementFromProcess(processId: String, statementId: String)
}

class AnalyserHandlerImpl(val name: String) extends AnalyserHandler with PreStart with PostStop with Logging {

  def this() = this("processor")

  val activeStatementMonitors = HashMap[String, Map[String, ActorRef]]()

  def addStatementToProcess(statement: Statement) {
    val statements = activeStatementMonitors.getOrElseUpdate(statement.processId, new HashMap[String, ActorRef])
    val currentStatementAnalyser = statements.get(statement.statementId)
    stopStatementAnalyser(currentStatementAnalyser)

    debug("Starting statement  " +statement.statementId + " for process: " + statement.processId + " in context " + TypedActor.context.self)
    val newStatementAnalyser = TypedActor.context.actorOf(Props(statement.getStatementAnalyser), name = statement.processId)
    statements.put(statement.statementId, newStatementAnalyser)
  }

  def removeStatementFromProcess(processId: String, statementId: String) {
    val currentAnalyser = activeStatementMonitors(processId).remove(statementId)
    stopStatementAnalyser(currentAnalyser)
  }

  def stopStatementAnalyser(process: Option[ActorRef]) {
    process match {
      case Some(actor) => TypedActor.context.stop(actor)
      case None => debug("No statement analyser to stop")
    }
  }

  override def preStart(): Unit = {
    debug("Starting analyser handler")
  }

  override def postStop(): Unit = {
    debug("Stopping analyser handler")
//    TypedActor.context.children.foreach { child => TypedActor.context.stop(child) }
  }
}