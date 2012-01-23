package se.aorwall.bam.process

import akka.event.ActorEventBus
import akka.event.LookupClassification
import akka.event.ScanningClassification
import akka.event.SubchannelClassification
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model._
import akka.event.EventBus

/**
 * This is just a first implementation of the event bus. 
 * 
 * Will try to optimize this by classify the event type first and the name after
 */
object ProcessorEventBus extends ActorEventBus with ScanningClassification { 
  
    type Event = events.Event
    type Classifier = EventClassifier
    
    protected def compareClassifiers(a: Classifier, b: Classifier): Int = a compareTo b
  
    protected def publish(event: events.Event, subscriber: Subscriber): Unit = {
      subscriber ! event
    }
    
    protected def matches(classifier: Classifier, event: Event): Boolean = {
      
      if (classifier.clazz == event.getClass) {
        classifier.eventName match {
          case Some(name) if (name == event.name) => true
          case Some(name) => false
          case _ => true
        }        
      } else {
        false
      }      
    }
}

case class EventClassifier(clazz: Class[_ <: Event], eventName: Option[String]) {

	def compareTo(other: EventClassifier): Int = {
		val comp = clazz.getName.compareTo(other.clazz.getName) 

		if(comp == 0) {
			eventName match {
				case Some(name) => other.eventName match {
					case Some(otherName) => name compareTo otherName
					case None => 1
			}
			case None => -1  
			}
		} else {
			comp
		}
	}
}

