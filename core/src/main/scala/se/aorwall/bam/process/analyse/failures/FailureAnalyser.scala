package se.aorwall.bam.process.analyse.failures

import scala.collection.immutable.TreeMap
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam._
import se.aorwall.bam.model.State
import se.aorwall.bam.process.analyse.window.Window
import se.aorwall.bam.process.analyse.Analyser
import akka.actor.ActorRef
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

class FailureAnalyser (
    override val subscriptions: List[Subscription], 
    override val channel: String, 
    override val category: Option[String],
    val maxOccurrences: Long)
  extends Analyser(subscriptions, channel, category) 
  with Window 
  with ActorLogging {

  type T = Event with HasState
  type S = State

  var failedEvents = new TreeMap[Long, State] ()

  override def receive = {
    case event: Event with HasState => process(event) 
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.warning("{} is not an event with a state", msg)
  }

  override protected def process(event: T) {
    event.state match {
      case model.Failure => {
        // Add new
        failedEvents += (event.timestamp -> event.state)  // TODO: What if two activites have the same timestamp?
	
        // Remove old
        val inactiveEvents = getInactive(failedEvents)
	
        failedEvents = failedEvents.drop(inactiveEvents.size)
	
        log.debug("no of failed events from subscriptions {}: {}", subscriptions, failedEvents.size)
	
        if(failedEvents.size > maxOccurrences) {
          alert("%s failed events from subscriptions %s is more than allowed (%s)".format(failedEvents.size, subscriptions, maxOccurrences))
        } else {
          backToNormal()
        }
      }

      case _ => 
    }
  }
}