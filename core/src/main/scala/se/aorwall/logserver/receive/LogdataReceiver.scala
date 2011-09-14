package se.aorwall.logserver.receive

import akka.actor.{Actor}
import grizzled.slf4j.Logging
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.storage.Storing
import se.aorwall.logserver.model.{Log, LogEvent}
import akka.routing.{SmallestMailboxSelector, FixedSizeCapacitor, DefaultActorPool}
import akka.actor.Actor._
import akka.config.Supervision.OneForOneStrategy

/**
 * 1. Receive log data object
 * 2. Save in DB
 * 3. Send to process actors
 */
class LogdataReceiver extends Actor with Storing with Logging {
  self.faultHandler = new OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  def receive = {
      case log: Log => processLogdata(log)
  }

  def processLogdata(logdata: Log) = {

    //TODO storage.storeLogdata(logdata)

    val logEvent = new LogEvent(logdata.correlationId, logdata.componentId, logdata.timestamp, logdata.state)
    info("created logEvent: " + logEvent)

    val monitoredProcesses = Actor.registry.actorsFor[ProcessActor] // TODO: Try using a listener to the ActorRegistry instead
    debug("found " + monitoredProcesses.size + " process actors")

    // send logevent object to all process actors
    for(processActor <- monitoredProcesses) processActor ! logEvent

  }

}

class LogdataReceiverPool extends Actor with DefaultActorPool
                               with FixedSizeCapacitor
                               with SmallestMailboxSelector
{
   def receive = _route
   def limit = 5
   def partialFill = true
   def selectionCount = 1
   def instance = actorOf[LogdataReceiver]
}
