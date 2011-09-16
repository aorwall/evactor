package se.aorwall.logserver.test

import se.aorwall.logserver.model.Activity
import akka.actor.Actor
import grizzled.slf4j.Logging

class ResponseActor extends Actor with Logging {

   def receive = {
    case a: Activity => {
      debug("Received activity with state " + a.state + " " + (System.currentTimeMillis() - a.startTimestamp) + " ms after the request was created, overhead: " + ((System.currentTimeMillis() - a.startTimestamp) - (a.endTimestamp - a.startTimestamp)) + " ms")
      Main.count()
    }
  }

}