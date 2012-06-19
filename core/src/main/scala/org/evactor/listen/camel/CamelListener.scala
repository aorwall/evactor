package org.evactor.listen.camel

import org.evactor.monitor.Monitored
import akka.actor.{ActorLogging, ActorRef}
import org.evactor.listen.Listener
import akka.camel.{Consumer, CamelMessage}
import org.evactor.model.events.LogEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.codehaus.jackson.map.{Module, DeserializationConfig}
import com.fasterxml.jackson.module.scala.{JacksonModule, DefaultScalaModule}

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
