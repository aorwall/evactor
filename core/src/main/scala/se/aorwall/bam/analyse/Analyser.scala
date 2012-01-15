package se.aorwall.bam.analyse

import se.aorwall.bam.model.statement.Statement
import collection.mutable.HashMap
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Timeout
import akka.actor._
import akka.actor.Props._
import se.aorwall.bam.model.events.Event

class Analyser extends Actor with Logging {

  val activeStatements = HashMap[String, Statement]()

  def receive = {
    case event: Event => sendToAnalysers(event)
    case statement: Statement => addStatement(statement)
    case statementId: String => removeStatement(statementId)
    case msg => info(context.self + " can't handle: " + msg)
  }

  def sendToAnalysers(event: Event) {
    debug(context.self + " sending event to " + context.children.size + " analysers")
    context.children.foreach(analyser => analyser ! event)
  }

  def addStatement(statement: Statement) {
    val currentAnalyser = context.actorFor(statement.statementId)
    stopStatementAnalyser(currentAnalyser)

    debug(context.self + " starting statement  " + statement.statementId + " for event with name " + statement.eventName + " in context " + context.self)
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