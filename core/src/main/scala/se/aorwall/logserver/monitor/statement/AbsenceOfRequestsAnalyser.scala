package se.aorwall.logserver.monitor.statement

import se.aorwall.logserver.model.Activity
import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Scheduler}
import java.util.concurrent.{ScheduledFuture, TimeUnit}

class AbsenceOfRequestsAnalyser (val processId: String, val alerter: ActorRef, val timeframe: Long)
  extends StatementAnalyser (processId, alerter) with Logging {

  var scheduledFuture: Option[ScheduledFuture[AnyRef]] = None

  def analyse(activity: Activity) {
    debug("Received: " + activity)

    if(activity.activityId == "")
      alert("No activities within the timeframe " + timeframe + "ms")
    else
      backToNormal("Back to normal")

    scheduledFuture match {
      case Some(s) => s.cancel(true)
      case None => warn("No scheduler set in Absence of request monitor for process: " + processId)
    }
    startScheduler()
  }

  def startScheduler() {
    scheduledFuture = Some(Scheduler.schedule(self, new Activity(processId, "", 0, 0, 0), timeframe, timeframe, TimeUnit.MILLISECONDS))
  }

  override def preStart() {
    trace("Starting statement monitor with id " + self.id)
    startScheduler()
  }

  override def postStop() {
    trace("Stopping statement monitor with id " + self.id)
    scheduledFuture match {
      case Some(s) => s.cancel(true)
      case None => warn("No scheduler set in Absence of request monitor for process: " + processId)
    }
  }

}
