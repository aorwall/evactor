package se.aorwall.bam.model.statement

import se.aorwall.bam.analyse.statement.AbsenceOfRequestsAnalyser
import akka.actor.Actor._

class AbsenceOfRequests (processId: String, statementId: String, alertEndpoint: String, timeFrame: Long)
  extends Statement(processId, statementId, alertEndpoint) {

  def getStatementAnalyser = new AbsenceOfRequestsAnalyser(processId, timeFrame)

}