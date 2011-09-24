package se.aorwall.logserver.model.statement

import akka.actor.{Actor, ActorRef}
import Actor._
import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.logserver.monitor.statement.LatencyAnalyser
import se.aorwall.logserver.monitor.statement.window.{TimeWindow, LengthWindow}

class Latency (statementId: String, alertEndpoint: String, maxLatency: Long, window: Option[WindowConf]) extends Statement(statementId, alertEndpoint) {

    def createActor(processId: String) = window match {
      case length: LengthWindowConf => actorOf(new LatencyAnalyser(processId, createAlerter(), maxLatency) with LengthWindow {override val noOfRequests = length.noOfRequests})
      case time: TimeWindowConf => actorOf(new LatencyAnalyser(processId, createAlerter(), maxLatency) with TimeWindow {override val timeframe = time.timeframe})
      case None =>  actorOf (new LatencyAnalyser(processId, createAlerter(), maxLatency))
    }
}