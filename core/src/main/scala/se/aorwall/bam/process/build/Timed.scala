package se.aorwall.bam.process.build

import akka.actor.Cancellable
import akka.util.duration.longToDurationLong
import se.aorwall.bam.model.Timeout
import akka.actor.Actor

trait Timed extends Actor  {
  
  def timeout: Option[Long]
  
  var scheduledTimeout:Option[Cancellable] = None 
      
  override def preStart() {
    scheduledTimeout = timeout match {
      case Some(timeout) => Some(context.system.scheduler.scheduleOnce(timeout seconds, self, Timeout))
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
