package se.aorwall.logserver.model.statement

import akka.actor.Actor
import Actor._
import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.logserver.analyse.statement.LatencyAnalyser
import se.aorwall.logserver.analyse.statement.window.{TimeWindow, LengthWindow}

class Latency (processId: String, statementId: String, alertEndpoint: String, maxLatency: Long, window: Option[WindowConf])
  extends Statement(processId, statementId, alertEndpoint) {

   def getStatementAnalyser = window match {
      case Some(length: LengthWindowConf) => new LatencyAnalyser(processId, maxLatency) with LengthWindow {override val noOfRequests = length.noOfRequests}
      case Some(time: TimeWindowConf) => new LatencyAnalyser(processId, maxLatency) with TimeWindow {override val timeframe = time.timeframe}
      case None => new LatencyAnalyser(processId, maxLatency)
   }
}