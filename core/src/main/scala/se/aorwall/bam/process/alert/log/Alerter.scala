package se.aorwall.bam.process.alert.log

import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName
import se.aorwall.bam.process.alert.Alerter
import se.aorwall.bam.model.events.Event
import akka.actor.ActorLogging

class LogAlerter (
    override val name: String, 
    val eventName: Option[String]) 
  extends Alerter (name, eventName) 
  with ActorLogging {
  
  type T = Event
    
  protected def process(event: Event) {
    log.error("ALERT: " + event)
  }

}