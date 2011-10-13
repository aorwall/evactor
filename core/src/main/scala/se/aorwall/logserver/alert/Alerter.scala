package se.aorwall.logserver.alert

import akka.actor.Actor
import akka.camel.Producer
import grizzled.slf4j.Logging

class Alerter (endpoint: String) extends Actor with Producer with Logging {
  def endpointUri = endpoint
  self.id = endpointUri
  override def oneway = true

 /* override protected def receiveBeforeProduce = {
    case alert: Alert => {
      info("Alerter: " + alert)
      val msg = new Message
      msg.setBody(alert)
      msg
    }
    case msg: Message => {
      info("Alerter: " + msg)
      msg
    }
  }*/

  override def preStart() {
    trace("Starting alerter with endpoint " + endpointUri)
  }

  override def postStop() {
    trace("Stopping alerter with endpoint " + endpointUri)
  }
}
