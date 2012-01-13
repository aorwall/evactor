package se.aorwall.bam.alert

import akka.actor.Actor
import grizzled.slf4j.Logging

class Alerter (endpoint: String) extends Actor with Logging {

  def receive = {
    case _ => warn("Not yet implemented (waiting for the Camel implementaiton in Akka 2.0)")
  }

  override def preStart() {
    trace("Starting alerter with endpoint " + endpoint)
  }

  override def postStop() {
    trace("Stopping alerter with endpoint " + endpoint)
  }
  
}
