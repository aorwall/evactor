package se.aorwall.logserver.model.statement

import window.{TimeWindowConf, LengthWindowConf, WindowConf}
import se.aorwall.logserver.monitor.statement.FailureAnalyser
import se.aorwall.logserver.monitor.statement.window.{LengthWindow, TimeWindow}
import akka.actor.Actor._

class Failure (processId: String, statementId: String, alertEndpoint: String, states: List[Int], maxOccurrences: Long, window: Option[WindowConf])
  extends Statement(processId, statementId, alertEndpoint) {

    val statementMonitor =  window match {
      case Some(length: LengthWindowConf) => actorOf(new FailureAnalyser(processId, alerter, states, maxOccurrences) with LengthWindow {override val noOfRequests = length.noOfRequests})
      case Some(time: TimeWindowConf) => actorOf(new FailureAnalyser(processId, alerter, states, maxOccurrences) with TimeWindow {override val timeframe = time.timeframe})
      case None => actorOf(new FailureAnalyser(processId, alerter, states, maxOccurrences))
    }
    statementMonitor.id = processId
}