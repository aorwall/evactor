package se.aorwall.bam.process

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.event.ActorEventBus
import akka.event.LookupClassification
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events
import akka.actor.ActorSystemImpl
import akka.actor.ExtendedActorSystem

/**
 * This is a first implementation of the event bus for only sending events
 * to subscribing processors. A processor can subscribe to either one type of
 * events ("event-classname/path/event") or all event names directly under a 
 * specified path ("event-classname/path/*"). 
 * 
 * Will be extended... 
 * 
 * It's used as an extension atm, maybe change to an actor instead to be 100%
 * thread safe...
 * 
 */*/

object ProcessorEventBusExtension extends ExtensionId[ProcessorEventBus] with ExtensionIdProvider {

  override def lookup = ProcessorEventBusExtension

  override def createExtension(system: ExtendedActorSystem) = new ProcessorEventBus
}

class ProcessorEventBus extends Extension with ActorEventBus with LookupClassification with Logging { 
    
    type Event = events.Event
    type Classifier = String
    
    val mapSize = 100
    
    def classify(event: Event): Classifier = event.path
    
    protected def compareClassifiers(a: Classifier, b: Classifier): Int = a compareTo b
    
    protected def publish(event: events.Event, subscriber: Subscriber): Unit = {
      trace("publishing event " + event.name + " to " + subscriber)
      subscriber ! event
    }
    
    override def publish(event: Event): Unit = {       
       val name = classify(event)
       
       // send to all subscribers who subscribed to this specific event name
	    val i = subscribers.valueIterator(name)
	    	    
	    while (i.hasNext) publish(event, i.next())
       
	    // send to all subscribers who subscribed to all events at this path
       val slashIndex = name.lastIndexOf("/")
       if(slashIndex > 0) {         
         val path = name.substring(0, slashIndex+1) + "*"
         val j = subscribers.valueIterator(path)
                  
         while (j.hasNext){
           publish(event, j.next())
         } 
       }
       
       // and send to all subscribers that hasn't specified a event name at all in the subscription
       val k = subscribers.valueIterator("")
       while (k.hasNext) publish(event, k.next())
       
    }
}

trait UseProcessorEventBus extends Actor {
 
  private[process] val bus = ProcessorEventBusExtension(context.system)
  
}

/**
 * Trait extended by actors publishing events to the processor event bus
 */
trait Publisher extends UseProcessorEventBus {
   
  def publish(event: events.Event) {
    bus.publish(event)
  }
  
}

/**
 * Trait extended by actors subscribing to the processor event bus  
 */
trait Subscriber extends UseProcessorEventBus {
   
  def subscribe(subscriber: ActorRef, classifier: String) {
    bus.subscribe(subscriber, classifier)
  }
  
  def unsubscribe(subscriber: ActorRef, classifier: String) {
    bus.unsubscribe(subscriber, classifier)
  }
    
}