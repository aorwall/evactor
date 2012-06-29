package org.evactor.listen.camel

import org.evactor.monitor.Monitored
import akka.actor.{ActorLogging, ActorRef}
import org.evactor.listen.Listener
import akka.camel.{Consumer, CamelMessage}

/**
 * User: anders
 */

class CamelListener(sendTo: ActorRef, theEndpointUri: String)
  extends Listener with Monitored with ActorLogging with Consumer {

  def endpointUri = theEndpointUri

  def receive = {
    case msg: CamelMessage => {
      log.debug("received %s" format msg.bodyAs[String])
      sendTo ! msg.body
    }
  }
}
