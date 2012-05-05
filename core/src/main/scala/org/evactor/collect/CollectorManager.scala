package org.evactor.collect

import org.evactor.listen.Listener
import org.evactor.listen.ListenerConfiguration
import org.evactor.model.events.Event
import org.evactor.process.Publication
import org.evactor.transform.Transformer
import org.evactor.transform.TransformerConfiguration

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.util.duration._

class CollectorManager extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: IllegalArgumentException => Stop
    case _: Exception => Restart
  }

  def receive = {
    case collector: AddCollector => addCollector(collector)
    case id: String => removeCollector(id)
    case msg => log.debug("can't handle {}", msg)
  }
  
  private[this] def addCollector(c: AddCollector) {
    try {
      log.debug("starting collector with configuration: {}", c)
      
      context.actorOf(Props(new Collector(c.listener, c.transformer, c.publication)), name = c.name)
      sender ! Status.Success
    } catch {
      case e: Exception => {
        log.warning("Starting collector with name {} failed. {}", c.name, e)
        sender ! Status.Failure(e)
      }
    }
  }
  
  private[this] def removeCollector(id: String) {
    
  }
}

case class AddCollector(
    val name: String,
    val listener: Option[ListenerConfiguration], 
    val transformer: Option[TransformerConfiguration], 
    val publication: Publication)
