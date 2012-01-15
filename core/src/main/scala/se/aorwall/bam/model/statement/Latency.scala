package se.aorwall.bam.model.statement

import akka.actor.Actor
import Actor._
import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.bam.analyse.statement.LatencyAnalyser
import se.aorwall.bam.analyse.statement.window.{TimeWindow, LengthWindow}

class Latency (eventName: String, statementId: String, alertEndpoint: String, maxLatency: Long, window: Option[WindowConf])
  extends Statement(eventName, statementId, alertEndpoint) {

   def getStatementAnalyser = window match {
      case Some(length: LengthWindowConf) => new LatencyAnalyser(eventName, maxLatency) with LengthWindow {override val noOfRequests = length.noOfRequests}
      case Some(time: TimeWindowConf) => new LatencyAnalyser(eventName, maxLatency) with TimeWindow {override val timeframe = time.timeframe}
      case None => new LatencyAnalyser(eventName, maxLatency)
   }
}