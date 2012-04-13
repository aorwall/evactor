package se.aorwall.bam.process.alert.log

import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.alert.Alerter
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

class LogAlerter (
    override val subscriptions: List[Subscription])
  extends Alerter (subscriptions) 
  with ActorLogging {
  
  type T = Event
    
  protected def process(event: Event) {
    log.error("ALERT: " + event)
  }

}