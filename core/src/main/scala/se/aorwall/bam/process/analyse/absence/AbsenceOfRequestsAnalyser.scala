package se.aorwall.bam.process.analyse.absence

import akka.actor.{Cancellable}
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.analyse.Analyser
import se.aorwall.bam.process.CheckEventName
import se.aorwall.bam.model.Timeout
import akka.actor.ActorRef
import akka.actor.ActorLogging

/**
 * TODO: Check event.timestamp to be really sure about the timeframe between events 
 */
class AbsenceOfRequestsAnalyser (name: String, eventName: Option[String], val timeframe: Long)
  extends Analyser(name, eventName) with CheckEventName with ActorLogging {
  
  type T = Event
  
  var scheduledFuture: Option[Cancellable] = None
    
  override def receive  = {
    case event: T => if (handlesEvent(event)) process(event) 
    case Timeout => alert("", "No events within the timeframe " + timeframe + "ms")
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  protected def process(event: T) {
    log.debug("received: " + event)

    // TODO: Check event.timestamp to be really sure about the timeframe between events
    backToNormal("", "Back to normal")
    
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => log.warning("no scheduler set in Absence of request analyse for event: " + eventName)
    }
    startScheduler()
  }

  def startScheduler() {
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeframe milliseconds, self, Timeout))
  }
  
  override def preStart() {
    log.debug("Starting with timeframe: " + timeframe)
    startScheduler()
  }

  override def postStop() {
    log.debug("Stopping...")
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => log.warning("No scheduler set in Absence of request analyse for event: " + eventName)
    }
  }

}
