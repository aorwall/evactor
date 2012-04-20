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
import se.aorwall.bam.model.events
import akka.actor.ActorSystemImpl
import akka.actor.ExtendedActorSystem
import scala.collection.JavaConversions._
import akka.actor.ActorLogging

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

// TODO: Subclassifier or something...
class ProcessorEventBus extends Extension with ActorEventBus with LookupClassification { 
  
  type Event = events.Event
  type Classifier = Subscription
    
  val mapSize = 100
    
  def classify(event: Event): Classifier = new Subscription(Some(event.getClass.getSimpleName), Some(event.channel), event.category)
    
  //protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.channel compareTo b.channel
    
  protected def publish(event: events.Event, subscriber: Subscriber) {
    //trace("publishing event {} to {}", event.toString, subscriber)
    subscriber ! event
  }
  
  override def publish(event: Event): Unit = {     
    val sub = classify(event)
       
    // send to all subscribers who has this specific event subscription if a category is specified
    if(sub.category.isDefined){
      val i = subscribers.valueIterator(sub)
      while (i.hasNext) { publish(event, i.next()) } 
    }
       
	// send to all subscribers who subscribed to all events on this channel
    if(sub.channel.isDefined){
      val channelSub = new Subscription(sub.eventType, sub.channel, None)
      val j = subscribers.valueIterator(channelSub)
      while (j.hasNext) { publish(event, j.next()) } 
    }
    
    // and send to all subscribers that hasn't specified 
    val k = subscribers.valueIterator(new Subscription(None, None, None))
    while (k.hasNext) { publish(event, k.next()) }
  }
}

object Subscriptions {
  
  def apply(eventType: String, channel: String): List[Subscription] = List(new Subscription(eventType, channel));
  
  def apply(eventType: String, channel: String, category: String): List[Subscription] = List(new Subscription(eventType, channel, category));
  
  def apply(subscriptions: java.util.Collection[Subscription]): List[Subscription] = subscriptions.toList
}

case class Subscription(
    val eventType: Option[String],
    val channel: Option[String],
    val category: Option[String]) {
  
  def this(eventType: String) = this(Some(eventType), None, None)
  
  def this(eventType: String, channel: String) = this(Some(eventType), Some(channel), None)
  
  def this(eventType: String, channel: String, category: String) = this(Some(eventType), Some(channel), Some(category))
   
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

  def subscribe(subscriber: ActorRef, subscriptions: List[Subscription]) {
    for(sub <- subscriptions){
      bus.subscribe(subscriber, sub)
    }
  }

  def unsubscribe(subscriber: ActorRef, subscriptions: List[Subscription]) {
    for(sub <- subscriptions){
      bus.unsubscribe(subscriber, sub)
    }
  }

}