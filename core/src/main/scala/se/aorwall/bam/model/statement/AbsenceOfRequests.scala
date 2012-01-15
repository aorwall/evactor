package se.aorwall.bam.model.statement

import se.aorwall.bam.analyse.statement.AbsenceOfRequestsAnalyser
import akka.actor.Actor._

class AbsenceOfRequests (eventName: String, statementId: String, alertEndpoint: String, timeFrame: Long)
  extends Statement(eventName, statementId, alertEndpoint) {

  def getStatementAnalyser = new AbsenceOfRequestsAnalyser(eventName, timeFrame)

}