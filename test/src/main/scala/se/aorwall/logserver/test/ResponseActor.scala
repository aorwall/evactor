package se.aorwall.logserver.test

import se.aorwall.logserver.model.Activity
import akka.actor.Actor

class ResponseActor extends Actor {

   def receive = {
    case a: Activity => print(a)
  }

}