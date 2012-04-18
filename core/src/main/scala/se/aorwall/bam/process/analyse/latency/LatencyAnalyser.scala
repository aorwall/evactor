package se.aorwall.bam.process.analyse.latency

import collection.immutable.TreeMap
import akka.actor.ActorRef
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.process.analyse.Analyser
import se.aorwall.bam.process.analyse.window.Window
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

class LatencyAnalyser(
    override val subscriptions: List[Subscription], 
    override val channel: String, 
    override val category: Option[String],
    val maxLatency: Long)
  extends Analyser(subscriptions, channel, category) with Window with ActorLogging {

  type T = Event with HasLatency
  type S = Long

  var events = new TreeMap[Long, Long]()
  var sum = 0L

  override def receive = {
    case event: Event with HasLatency => process(event) 
    case actor: ActorRef => testActor = Some(actor) 
    case msg => log.warning("{} is not an event with a latency value", msg)
  }
  
  override protected def process(event: T) {

   log.debug("received: {}", event)
	
	// Add new
	val latency = event.latency
	events += (event.timestamp -> latency)
	sum += latency
	
	// Remove old
	val inactiveEvents = getInactive(events)
	events = events.drop(inactiveEvents.size)
	sum += inactiveEvents.foldLeft(0L) {
	  case (a, (k, v)) => a - v
	}
	
	// Count average latency
	val avgLatency = if (sum > 0) {
	  sum / events.size
	} else {
	  0
	}
	
	log.debug("sum: {}, no of events: {}, avgLatency: {}", sum, events.size, avgLatency)
	
	if (avgLatency > maxLatency) {
	  alert("Average latency %s ms is higher than the maximum allowed latency %s ms".format(avgLatency, maxLatency), Some(event))
	} else {
	  backToNormal()
	}    
  }
}