package se.aorwall.logserver.receive

import akka.actor.{Actor}
import grizzled.slf4j.Logging
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.model.{Log}
import akka.config.Supervision.OneForOneStrategy
import akka.camel.{Message, Consumer}

/**
 * 1. Receive log data object
 * 2. Save in DB
 * 3. Send to process actors
 */
class LogdataReceiver(camelUri: String) extends Actor with Consumer with Logging {

  def endpointUri = camelUri

  self.faultHandler = new OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  def receive = {
    case msg: Message => processLogdata(getLogFromCamelMessage(msg))
    case log: Log => processLogdata(log)
  }

  // TODO: This method should handle different data types in the message object
  def getLogFromCamelMessage(msg: Message): Log = {
    msg.bodyAs[Log]
  }

  def processLogdata(logevent: Log) = {

    val monitoredProcesses = Actor.registry.actorsFor[ProcessActor]
    debug("found " + monitoredProcesses.size + " process actors")

    // send logevent object to all process actors
    for (processActor <- monitoredProcesses) processActor ! logevent
  }

}
