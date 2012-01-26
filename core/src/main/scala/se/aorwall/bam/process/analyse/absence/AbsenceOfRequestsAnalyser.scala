package se.aorwall.bam.process.analyse.absence

import grizzled.slf4j.Logging
import akka.actor.{Cancellable}
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.analyse.Analyser
import se.aorwall.bam.process.CheckEventName
import se.aorwall.bam.model.Timeout
import akka.actor.ActorRef

/**
 * TODO: Check event.timestamp to be really sure about the timeframe between events 
 */
class AbsenceOfRequestsAnalyser (name: String, eventName: Option[String], val timeframe: Long)
  extends Analyser(name, eventName) with CheckEventName with Logging {
  
  type T = Event
  
  var scheduledFuture: Option[Cancellable] = None
    
  override def receive  = {
    case event: T => if (handlesEvent(event)) process(event) 
    case Timeout => alert("No events within the timeframe " + timeframe + "ms")
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  protected def process(event: T) {
    debug(context.self + " received: " + event)

    // TODO: Check event.timestamp to be really sure about the timeframe between events
    backToNormal("Back to normal")
    
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => warn(context.self + " no scheduler set in Absence of request analyse for event: " + eventName)
    }
    startScheduler()
  }

  def startScheduler() {
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, Timeout))
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
