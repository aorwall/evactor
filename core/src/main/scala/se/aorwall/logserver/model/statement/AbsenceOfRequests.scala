package se.aorwall.logserver.model.statement

import se.aorwall.logserver.monitor.statement.AbsenceOfRequestsAnalyser
import akka.actor.Actor._

class AbsenceOfRequests (statementId: String, alertEndpoint: String, timeFrame: Long) extends Statement(statementId, alertEndpoint) {

   def createActor(processId: String) = actorOf(new AbsenceOfRequestsAnalyser(processId, createAlerter, timeFrame))

}