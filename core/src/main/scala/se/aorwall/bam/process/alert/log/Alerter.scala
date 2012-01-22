package se.aorwall.bam.process.alert.log
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName
import se.aorwall.bam.process.alert.Alerter
import se.aorwall.bam.model.events.Event

class LogAlerter (override val name: String, val eventName: Option[String]) extends Alerter (name, eventName) with Logging {
  
  type T = Event
    
  protected def process(event: Event) = {
    error("ALERT: " + event)
  }
  
}