package se.aorwall.logserver.storage

import akka.actor.Actor
import grizzled.slf4j.Logging

trait Storing extends Logging {

   //TODO Fix better solution
   def storage = {
     Actor.registry.typedActorsFor(classOf[LogStorage]).head match  {
       case a: LogStorage => a
     }
   }
}