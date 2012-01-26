package se.aorwall.bam.process

import akka.event.ActorEventBus
import akka.event.LookupClassification
import akka.event.ScanningClassification
import akka.event.SubchannelClassification
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model._
import akka.event.EventBus

/**
 * This is a first implementation of the event bus for only sending events
 * to subscribing processors. A processor can subscribe to either one type of
 * events ("event-classname/path/event") or all event names directly under a 
 * specified path ("event-classname/path/*"). 
 * 
 * Will be extended... 
 * 
 */*/
object ProcessorEventBus extends ActorEventBus with LookupClassification { 
    
    type Event = events.Event
    type Classifier = String
    
    val mapSize = 100
    
    def classify(event: Event): Classifier = event.path
    
    protected def compareClassifiers(a: Classifier, b: Classifier): Int = a compareTo b
    
    protected def publish(event: events.Event, subscriber: Subscriber): Unit = {
      subscriber ! event
    }
    
    override def publish(event: Event): Unit = {       
       val name = classify(event)
       
       // send to all subscribers who subscribed to this specific event name
	    val i = subscribers.valueIterator(name)
	    
	    while (i.hasNext) publish(event, i.next())
       
	    // send to all subscribers who subscribed to all events at this path
       val slashIndex = name.indexOf("/")
       if(slashIndex > 0) {         
         val path = name.substring(0, slashIndex+1) + "*"
         val j = subscribers.valueIterator(path)
                  
         while (j.hasNext) publish(event, j.next())
       } 
       
       // and send to all subscribers that hasn't specified a event name at all in the subscription
       val k = subscribers.valueIterator("")
       while (k.hasNext) publish(event, k.next())
       
    }
}
