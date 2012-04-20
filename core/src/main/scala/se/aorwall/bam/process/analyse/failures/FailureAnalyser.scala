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
import scala.collection.immutable.TreeSet
import se.aorwall.bam.model

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

  var allEvents = new TreeMap[Long, State]
  
  override def receive = {
    case event: Event with HasState => process(event) 
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.warning("{} is not an event with a state", msg)
  }

  override protected def process(event: T) {
    allEvents += (event.timestamp -> event.state)
      
    // Remove old
    val inactiveEvents = getInactive(allEvents)	
    allEvents = allEvents.drop(inactiveEvents.size)
	
    val failedEvents = allEvents.count { _._2 match {
      case model.Failure => true
      case _ => false
   	 }
    }
 
    log.debug("no of failed events from subscriptions {}: {}", subscriptions, failedEvents)

    if(failedEvents > maxOccurrences) {
      alert("%s failed events from subscriptions %s is more than allowed (%s)".format(failedEvents, subscriptions, maxOccurrences), Some(event))
    } else {
      backToNormal()
    }
  
  }
}