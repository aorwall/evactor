package se.aorwall.bam.process

import akka.util.duration._
import akka.actor.Cancellable
import akka.actor.Actor

trait Timed extends Actor  {
  
  var scheduledTimeout:Option[Cancellable] = None 
  var _timeout: Option[Long] = None
    
  override def preStart() {
    scheduledTimeout = _timeout match {
      case Some(timeout) => Some(context.system.scheduler.scheduleOnce(timeout seconds, self, new Timeout))
      case _ => None
    }
  }
  
  override def postStop() {
	 scheduledTimeout match {
	    case Some(s) => s.cancel()
	    case _ =>
	 }
  }
  
}

case class Timeout() {

}