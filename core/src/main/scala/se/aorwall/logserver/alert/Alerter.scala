package se.aorwall.logserver.alert

import akka.actor.Actor
import akka.camel.{Message, Producer}
import se.aorwall.logserver.model.Alert
import grizzled.slf4j.Logging

class Alerter (endpoint: String) extends Actor with Producer with Logging {
  def endpointUri = endpoint
  self.id = endpointUri

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

  override protected def receiveAfterProduce = {
    // do not reply but forward result to target
    case msg => info("receiveAfterProduce: " + msg)
  }


  override def preStart = {
    trace("Starting alerter with endpoint " + endpointUri)
  }

  override def postStop = {
    trace("Stopping alerter with endpoint " + endpointUri)
  }
}
