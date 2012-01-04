package se.aorwall.logserver.analyse

import se.aorwall.logserver.model.statement.Statement
import collection.mutable.HashMap
import grizzled.slf4j.Logging
import se.aorwall.logserver.model.{Log, Activity}
import se.aorwall.logserver.process.Timeout
import akka.actor._
import akka.actor.Props._

/**
 * The reason to have both the AnalyserHandler and the ProcessAnalyser class is
 * to get the path analyse/processId/statementId. TODO: Find a better solution
 */
class ProcessAnalyser extends Actor with Logging {

  val activeStatements = HashMap[String, Statement]()

  def receive = {
    case activity: Activity => sendToAnalysers(activity)
    case statement: Statement => addStatement(statement)
    case statementId: String => removeStatement(statementId)
    case msg => info(context.self + " can't handle: " + msg)
  }

  def sendToAnalysers(activity: Activity) {
    debug(context.self + " sending activity to " + context.children.size + " statement analysers")
    context.children.foreach(analyser => analyser ! activity)
  }

  def addStatement(statement: Statement) {
    val currentAnalyser = context.actorFor(statement.statementId)
    stopStatementAnalyser(currentAnalyser)

    debug(context.self + " starting statement  " + statement.statementId + " for process: " + statement.processId + " in context " + context.self)
    context.actorOf(Props(statement.getStatementAnalyser), name = statement.statementId)

    activeStatements.put(statement.statementId, statement)
  }

  def removeStatement(statementId: String) {
    val currentAnalyser = context.actorFor(statementId)
    stopStatementAnalyser(currentAnalyser)
    activeStatements.remove(statementId)
  }

  def stopStatementAnalyser(process: ActorRef) {
    process match {
      case empty: EmptyLocalActorRef => debug("No statement analyser to stop")
      case actor: ActorRef => context.stop(actor)
    }
  }

  override def preStart = {
    trace(context.self+ " starting up " + activeStatements.size + " statement analysers")
    activeStatements.foreach(statement => TypedActor.context.actorOf(Props(statement._2.getStatementAnalyser), name = statement._1))
  }

  override def postStop = {
    trace(context.self+ " stopping...")
  }
}