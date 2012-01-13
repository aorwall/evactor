package se.aorwall.bam.collect

import akka.actor.{Actor}
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Processor
import se.aorwall.bam.model.{Log}

/**
 * 1. Receive log data object
 * 2. Send to process actors
 */
class Collector extends Actor with Logging {

  def receive = {
    case log: Log => processLogdata(log)
  }

  def processLogdata(logevent: Log) = {
    // TODO: store log event
    debug("Collecting: " + logevent)

    // send logevent object to all process actors
    context.actorSelection("../process/*") ! logevent
  }

  override def preStart = {
    trace("Starting collector")
  }

  override def postStop = {
    trace("Stopping collector")
  }
}
