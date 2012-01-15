package se.aorwall.bam.model.statement

import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.bam.analyse.statement.FailureAnalyser
import se.aorwall.bam.analyse.statement.window.{LengthWindow, TimeWindow}
import akka.actor.Actor._

class Failure (eventName: String, statementId: String, alertEndpoint: String, maxOccurrences: Long, window: Option[WindowConf])
  extends Statement(eventName, statementId, alertEndpoint) {

    def getStatementAnalyser = window match {
      case Some(length: LengthWindowConf) => new FailureAnalyser(eventName, maxOccurrences) with LengthWindow {override val noOfRequests = length.noOfRequests}
      case Some(time: TimeWindowConf) => new FailureAnalyser(eventName, maxOccurrences) with TimeWindow {override val timeframe = time.timeframe}
      case None => new FailureAnalyser(eventName, maxOccurrences)
    }
}