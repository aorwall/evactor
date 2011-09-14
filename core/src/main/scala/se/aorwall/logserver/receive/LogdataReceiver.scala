package se.aorwall.logserver.receive

import akka.actor.{Actor}
import grizzled.slf4j.Logging
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.storage.Storing
import se.aorwall.logserver.model.{Log, LogEvent}

/**
 * 1. Receive log data object
 * 2. Save in DB
 * 3. Send to process actors
 */
class LogdataReceiver extends Actor with Storing with Logging {

  def receive = {
      case log: Log => processLogdata(log)
  }

  def processLogdata(logdata: Log) = {

    //TODO storage.storeLogdata(logdata)

    val logEvent = new LogEvent(logdata.correlationId, logdata.componentId, logdata.timestamp, logdata.state)
    debug("created logEvent: " + logEvent)

    val monitoredProcesses = Actor.registry.actorsFor[ProcessActor] // TODO: Try using a listener to the ActorRegistry instead
    debug("found " + monitoredProcesses.size + " process actors")

    // send logevent object to all process actors
    for(processActor <- monitoredProcesses) processActor ! logEvent

  }

}