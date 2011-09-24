package se.aorwall.logserver.alert

import akka.actor.Actor
import akka.camel.{Message, Producer}

class Alerter (endpoint: String) extends Actor with Producer {
  def endpointUri = endpoint

  override protected def receiveBeforeProduce = {
    case msg: Message => { msg }
  }

}