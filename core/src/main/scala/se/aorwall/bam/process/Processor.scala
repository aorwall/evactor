package se.aorwall.bam.process

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.EmptyLocalActorRef
import akka.actor.InternalActorRef
import se.aorwall.bam.model.events.Event
import com.twitter.ostrich.stats.Stats

abstract class Processor (
    val subscriptions: List[Subscription]) 
  extends Actor
  with Subscriber 
  with ActorLogging {
  
  type T <: Event
  
  protected var testActor: Option[ActorRef] = None // actor used for testing
  
  def receive = {
    case event: T => process(event) // TODO: case event: T  doesn't work...
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }

  protected def process(event: T)
  
  override def preStart = {
    log.debug("subscribing to: {}", subscriptions)
    subscribe(context.self, subscriptions)
  }
  
  override def postStop = {
    log.debug("unsubscribing")
    unsubscribe(context.self, subscriptions)
  }
}

/**
 * Extended by processors that should be monitored by
 * Ostrich (https://github.com/twitter/ostrich)
 * 
 * TODO: This trait should use an extension instead to be able to
 * use other statistics libraries..
 * 
 */
trait Monitored extends Processor with ActorLogging {
  
  abstract override def preStart = {
    // set label context.self + running
    Stats.setLabel(context.self.toString, "running")
    super.preStart()
  }

  abstract override def postStop = {
    // set label context.self + stopped
    Stats.setLabel(context.self.toString, "stopped")
    super.postStop()
  }
  
  //TODO: This doesn't work, try intercepting the receive method instead if that's possible...
  
  //abstract override def receive = {
  //  case _: T => Stats.incr(context.self.toString)
    // count
    
 // }

}