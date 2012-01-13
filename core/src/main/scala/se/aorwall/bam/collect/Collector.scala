package se.aorwall.bam.collect

import akka.actor.{Actor}
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Processor
import se.aorwall.bam.model.events.Event

/**
 * Collects events
 */
class Collector extends Actor with Logging {

  def receive = {
    case event: Event => collect(event)
  }

  def collect(event: Event) = {
   
    debug(context.self + " collecting: " + event)

    // send event to processor
    context.actorFor("../process") ! event
    
    // TODO: Send to analyser
  }

  override def preStart = {
    trace(context.self + " starting...")
  }

  override def postStop = {
    trace(context.self + " stopping...")
  }
}
