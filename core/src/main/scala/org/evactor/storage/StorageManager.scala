package org.evactor.storage

import akka.actor.Actor
import org.evactor.process.UseProcessorEventBus
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.Props
import akka.actor.Status
import akka.util.duration._
import akka.actor.ActorLogging
import org.evactor.process.Subscriptions
import org.evactor.process.Subscription


/**
 * Manages the storage processors
 * 
 * TODO: Make sure that there are only one subscription for each channel
 */
class StorageManager extends Actor with ActorLogging {

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
    context.actorOf(Props(new StorageProcessorRouter(List(new Subscription(c.channel, None)), c.maxThreads)), name = name)
    sender ! Status.Success
  }
  
}