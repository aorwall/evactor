package se.aorwall.bam.process.alert
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName

abstract class Alerter (name: String, eventName: Option[String]) extends Processor (name)  with CheckEventName with Logging {

  
}