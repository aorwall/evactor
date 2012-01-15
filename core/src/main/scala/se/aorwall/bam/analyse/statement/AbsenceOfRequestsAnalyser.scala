package se.aorwall.bam.analyse.statement

import grizzled.slf4j.Logging
import akka.actor.{Cancellable}
import akka.util.duration._
import se.aorwall.bam.model.events.Event

/**
 * TODO: Check event.timestamp to be really sure about the timeframe between events 
 */
class AbsenceOfRequestsAnalyser (val eventName: String, val timeframe: Long)
  extends StatementAnalyser with Logging {
  
  type T = Event
  
  var scheduledFuture: Option[Cancellable] = None
  
  def analyse(event: T) {
    debug(context.self + " received: " + event)

    if(event.timestamp == 0) // TODO: Find a better solution
      alert("No events within the timeframe " + timeframe + "ms")
    else {
      // TODO: Check event.timestamp to be really sure about the timeframe between events
      backToNormal("Back to normal")
    }
    
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn(context.self + " no scheduler set in Absence of request analyse for event: " + eventName)
    }
    startScheduler()
  }

  def startScheduler() {
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, new Event(eventName, "", 0)))
  }
  
  override def preStart() {
    trace(context.self + " Starting with timeframe: " + timeframe)
    startScheduler()
  }

  override def postStop() {
    trace(context.self + " Stopping...")
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn(context.self + " No scheduler set in Absence of request analyse for event: " + eventName)
    }
  }

}
