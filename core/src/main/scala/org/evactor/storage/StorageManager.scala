package org.evactor.storage

import org.evactor.subscribe.Subscription
import akka.actor.SupervisorStrategy._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.Status
import scala.concurrent.duration._
import org.evactor.subscribe.Subscription
import org.evactor.Start
import com.typesafe.config.ConfigValueType
import org.evactor.ConfigurationException
import scala.collection.JavaConversions._
import org.evactor.subscribe.Subscriptions

/**
 * Manages the storage processors
 * 
 * TODO: Make sure that there are only one subscription for each channel
 */
class StorageManager extends Actor with ActorLogging {

  lazy val STORAGE = "evactor.storage"
  lazy val CHANNELS = "%s.channels".format(STORAGE)
  lazy val MAX_THREADS = "%s.maxThreads".format(STORAGE)
    
  val config = context.system.settings.config
  val maxThreads = if(config.hasPath(MAX_THREADS)) {
    config.getInt(MAX_THREADS)
  } else {
    5
  }
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: ConfigurationException => Stop
    case _: Exception => Restart
  }
  
  def receive = {
    case Start => start()
    case msg => log.warning("can't handle: {}", msg); sender ! Status.Failure
  }
  
  private[this] def start() {
    if(!config.hasPath(STORAGE)) throw new ConfigurationException("No storage configuration found (%s)".format(STORAGE))
    
    if(config.hasPath(CHANNELS)){
      val valType = config.getValue(CHANNELS).valueType
      if(valType == ConfigValueType.STRING && config.getString(CHANNELS).equals("all")){ 
        context.actorOf(Props(new StorageProcessorRouter(List(new Subscription(None)), maxThreads)), name = "all")
      } else if(valType == ConfigValueType.LIST) {
        val channels = config.getConfigList(CHANNELS)
        context.actorOf(Props(new StorageProcessorRouter(Subscriptions(channels.toList), maxThreads)))
      } else {
        throw new ConfigurationException("Path %s in invalid".format(CHANNELS))
      }
    }
  }
}