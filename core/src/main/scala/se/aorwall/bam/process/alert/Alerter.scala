package se.aorwall.bam.process.alert

import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.CheckEventName
import akka.actor.ActorLogging

abstract class Alerter (
    name: String, 
    eventName: Option[String]) 
  extends Processor (name)
  with CheckEventName 
  with ActorLogging {
  
}