package se.aorwall.logserver.analyse.statement

import se.aorwall.logserver.model.Activity
import grizzled.slf4j.Logging
import akka.actor.{Cancellable}
import akka.util.duration._

class AbsenceOfRequestsAnalyser (val processId: String, val timeframe: Long)
  extends StatementAnalyser (processId) with Logging {

  var scheduledFuture: Option[Cancellable] = None

  def analyse(activity: Activity) {
    debug("Received: " + activity)

    if(activity.activityId == "")
      alert("No activities within the timeframe " + timeframe + "ms")
    else
      backToNormal("Back to normal")

    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn("No scheduler set in Absence of request analyse for process: " + processId)
    }
    startScheduler()
  }

  def startScheduler() {
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, new Activity(processId, "", 0, 0, 0)))
  }

  override def preStart() {
    trace("Starting statement analyse")
    startScheduler()
  }

  override def postStop() {
    trace("Stopping statement analyse")
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn("No scheduler set in Absence of request analyse for process: " + processId)
    }
  }

}
