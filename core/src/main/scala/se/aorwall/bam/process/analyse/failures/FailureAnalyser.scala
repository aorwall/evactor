package se.aorwall.bam.process.analyse.failures

import scala.collection.immutable.TreeMap
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam._
import se.aorwall.bam.model.State
import se.aorwall.bam.process.analyse.window.Window
import se.aorwall.bam.process.analyse.Analyser
import akka.actor.ActorRef

class FailureAnalyser (name: String, eventName: Option[String], maxOccurrences: Long)
extends Analyser(name, eventName) with Window with Logging {

	type T = Event with HasState
	type S = State

	var failedEvents = new TreeMap[Long, State] ()
	
	override def receive = {
	    case event: Event with HasState => if (handlesEvent(event)) process(event) // TODO: case event: T  doesn't work...
	    case actor: ActorRef => testActor = Some(actor) 
	    case _ => // skip
	}
		
	protected def process(event: T) {
		event.state match {
			case model.Failure => {
	
				// Add new
				failedEvents += (event.timestamp -> event.state)  // TODO: What if two activites have the same timestamp?
	
				// Remove old
				val inactiveEvents = getInactive(failedEvents)
	
				failedEvents = failedEvents.drop(inactiveEvents.size)
	
				trace(context.self + " failedEvents: " + failedEvents)
				debug(context.self + " no of failed events with name " + eventName.get + ": " + failedEvents.size)
	
				if(failedEvents.size > maxOccurrences) {
					alert(failedEvents.size + " failed events with name " + eventName.get + " is more than allowed (" + maxOccurrences + ")")
				} else {
					backToNormal("Back to normal!")
				}
			}
			
			case _ => 
		}
	}
}