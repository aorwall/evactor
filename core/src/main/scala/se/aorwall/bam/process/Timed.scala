package se.aorwall.bam.process

import akka.util.duration._
import akka.actor.Cancellable
import akka.actor.Actor

trait Timed extends Actor  {
  
  var scheduledTimeout: Cancellable
  
  val timeout: Integer 
  
  /*
  override def preStart() {
    scheduledTimeout = Some(context.system.scheduler.scheduleOnce(timeout seconds, self, new Timeout))
  }
  */
  override def postStop() {
    scheduledTimeout.cancel()
  }
  
}

case class Timeout() {

}