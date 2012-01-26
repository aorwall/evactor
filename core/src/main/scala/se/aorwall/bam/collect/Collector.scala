package se.aorwall.bam.collect

import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Processor
import se.aorwall.bam.storage.Storage
import se.aorwall.bam.process.ProcessorEventBus

/**
 * Collecting events
 */
class Collector extends Actor with Storage with Logging {

  def receive = {
    case event: Event => collect(event)
  }

  def collect(event: Event) = {
   
    debug(context.self + " collecting: " + event)

    // save event and check for duplicates. TODO: Find a nice way of making this non blocking...
    if(storeEvent(event)) ProcessorEventBus.publish(event)
    else warn(context.self + " didn't send " + event)
    
  }

  private[this] def sendEvent(event: Event){    
    // send event to processors
    context.actorFor("../process") ! event    
  }
  
  override def preStart = {
    trace(context.self + " starting...")
  }

  override def postStop = {
    trace(context.self + " stopping...")
  }
}
