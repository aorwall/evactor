package se.aorwall.logserver.model.statement

import akka.actor.Actor
import Actor._
import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.logserver.monitor.statement.LatencyAnalyser
import se.aorwall.logserver.monitor.statement.window.{TimeWindow, LengthWindow}

class Latency (processId: String, statementId: String, alertEndpoint: String, maxLatency: Long, window: Option[WindowConf])
  extends Statement(processId, statementId, alertEndpoint) {

   val statementMonitor = window match {
      case Some(length: LengthWindowConf) => actorOf(new LatencyAnalyser(processId, alerter, maxLatency) with LengthWindow {override val noOfRequests = length.noOfRequests})
      case Some(time: TimeWindowConf) => actorOf(new LatencyAnalyser(processId, alerter, maxLatency) with TimeWindow {override val timeframe = time.timeframe})
      case None => actorOf (new LatencyAnalyser(processId, alerter, maxLatency))
   }
   statementMonitor.id = processId
}