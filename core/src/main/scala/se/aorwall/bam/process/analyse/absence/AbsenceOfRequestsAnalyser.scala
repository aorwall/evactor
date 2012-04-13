package se.aorwall.bam.process.analyse.absence

import akka.actor.{Cancellable}
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.analyse.Analyser
import se.aorwall.bam.model.Timeout
import akka.actor.ActorRef
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

/**
 * TODO: Check event.timestamp to be really sure about the timeframe between events 
 */
class AbsenceOfRequestsAnalyser (
    override val subscriptions: List[Subscription], 
    override val channel: String, 
    override val category: Option[String], 
    val timeframe: Long)
  extends Analyser(subscriptions, channel, category) 
  with ActorLogging {
  
  type T = Event
  
  var scheduledFuture: Option[Cancellable] = None
    
  override def receive  = {
    case event: T => process(event) 
    case Timeout => alert("No events within the timeframe %s ms".format(timeframe))
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }
  
  override protected def process(event: T) {
    log.debug("received: " + event)

    // TODO: Check event.timestamp to be really sure about the timeframe between events
    backToNormal()
    
    scheduledFuture match {
      case Some(s) => s.cancel()
      case None => log.warning("no scheduler set in Absence of request analyser with subscriptions: {}", subscriptions)
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
      case None => log.warning("No scheduler set in Absence of request analyse with subscriptions: {}", subscriptions)
    }
  }

}
