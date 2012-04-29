package org.evactor.storage

import akka.actor.Actor
import org.evactor.process.UseProcessorEventBus

/**
 * Manages the storage processors
 * 
 * TODO: Make sure that there are only one subscription for each channel
 */
class StorageManager extends Actor  {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: IllegalArgumentException => Stop
    case _: Exception => Restart
  }
  
  def receive = {
    case configuration: StorageProcessorConf => setStorageProcessor(configuration)
    // TODO: Remove configuration
    case msg => log.warning("can't handle: {}", msg); sender ! Status.Failure
  }
  
  private[this] def setStorageProcessor(c: StorageProcessorConf){
    val name = c.channel match {
      case Some(channel) => channel
      case None => "all"
    }
    context.actorOf(Props(new StorageProcessor(Subscriptions(c.channel), c.maxThreads)), name = name)
    sender ! Status.Success
  }
  
}