package se.aorwall.bam.analyse.statement

import grizzled.slf4j.Logging
import akka.actor.{Cancellable}
import akka.util.duration._
import se.aorwall.bam.model.events.Event

class AbsenceOfRequestsAnalyser (val processId: String, val timeframe: Long)
  extends StatementAnalyser (processId) with Logging {

  var scheduledFuture: Option[Cancellable] = None

  def analyse(event: Event) {
    debug("Received: " + event)

    if(event.id == "")
      alert("No events within the timeframe " + timeframe + "ms")
    else
      backToNormal("Back to normal")

    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn("No scheduler set in Absence of request analyse for process: " + processId)
    }
    startScheduler()
  }

  def startScheduler() {
  //  scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, new Activity(processId, "", 0, 0, 0)))
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
