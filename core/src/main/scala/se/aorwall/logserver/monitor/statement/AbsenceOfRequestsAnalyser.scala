package se.aorwall.logserver.monitor.statement

import se.aorwall.logserver.model.Activity
import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit
import akka.actor.{ActorRef, Scheduler, Actor}

class AbsenceOfRequestsAnalyser (val processId: String, val alerter: ActorRef, val timeframe: Long)
  extends StatementAnalyser (processId, alerter) with Logging {

  def analyse(activity: Activity) = {
    if(activity.correlationId == "")
      alert("No activities within the timeframe " + timeframe + "ms")
    else
      backToNormal("Back to normal")
  }

  override def preStart() = {
    Scheduler.schedule(self, new Activity(processId, "", 0, 0, 0), timeframe, timeframe, TimeUnit.MILLISECONDS)
  }

  //TODO: Scheduler.shutdown
}
