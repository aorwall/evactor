package se.aorwall.logserver.model.statement

import se.aorwall.logserver.monitor.statement.AbsenceOfRequestsAnalyser
import akka.actor.Actor._

class AbsenceOfRequests (processId: String, statementId: String, alertEndpoint: String, timeFrame: Long)
  extends Statement(processId, statementId, alertEndpoint) {

  val statementMonitor = actorOf(new AbsenceOfRequestsAnalyser(processId, alerter, timeFrame))
  statementMonitor.id = processId
}