package se.aorwall.bam.process.analyse.latency

import collection.immutable.TreeMap
import akka.actor.ActorRef
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.process.analyse.Analyser
import se.aorwall.bam.process.analyse.window.Window
import se.aorwall.bam.process.CheckEventName
import akka.actor.ActorLogging

class LatencyAnalyser(name: String, eventName: Option[String], maxLatency: Long)
  extends Analyser(name, eventName) with Window with CheckEventName with ActorLogging {

  type T = Event with HasLatency
  type S = Long

  var events = new TreeMap[Long, Long]()
  var sum = 0L

	override def receive = {
	    case event: Event with HasLatency => if (handlesEvent(event)) process(event) 
	    case actor: ActorRef => testActor = Some(actor) 
	    case _ => // skip
	}
  
  def process(event: T) {

   log.debug("received: " + event)
	
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
	
	log.debug("sum: " + sum + ", no of events: " + events.size + ", avgLatency: " + avgLatency)
	
	if (avgLatency > maxLatency) {
	  alert(event.name, "Average latency " + avgLatency + "ms is higher than the maximum allowed latency " + maxLatency + "ms")
	} else {
	  backToNormal(event.name, "back to normal!")
	}    
  }
}