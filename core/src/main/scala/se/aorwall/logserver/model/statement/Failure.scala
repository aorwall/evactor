package se.aorwall.logserver.model.statement

import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.logserver.analyse.statement.FailureAnalyser
import se.aorwall.logserver.analyse.statement.window.{LengthWindow, TimeWindow}
import akka.actor.Actor._

class Failure (processId: String, statementId: String, alertEndpoint: String, states: List[Int], maxOccurrences: Long, window: Option[WindowConf])
  extends Statement(processId, statementId, alertEndpoint) {

    def getStatementAnalyser = window match {
      case Some(length: LengthWindowConf) => new FailureAnalyser(processId, states, maxOccurrences) with LengthWindow {override val noOfRequests = length.noOfRequests}
      case Some(time: TimeWindowConf) => new FailureAnalyser(processId, states, maxOccurrences) with TimeWindow {override val timeframe = time.timeframe}
      case None => new FailureAnalyser(processId, states, maxOccurrences)
    }
}