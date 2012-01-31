package se.aorwall.bam.process.analyse

import scala.Predef._
import akka.actor.{Actor, ActorRef}
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Processor
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.process.CheckEventName
import akka.actor.ActorLogging
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.process.Publisher

abstract class Analyser(name: String, val eventName: Option[String]) extends Processor(name) with Publisher with ActorLogging  {
    
  var triggered = false //TODO: use FSM for this
  
  protected def alert(message: String) {
    if (!triggered) {
      log.warning("Alert: " + message)

      triggered = true
      sendAlert(message)
    }
  }

  protected def backToNormal(message: String) {
    if (triggered) {
      log.info("Back to normal: " + message)

      triggered = false
      sendAlert(message)
    }
  }

  def sendAlert(message: String) {
    val currentTime = System.currentTimeMillis
    val alert = new AlertEvent(name, currentTime.toString, currentTime, triggered, message)
    
    publish(alert)

    // If a test actor exists
    testActor match {
      case Some(actor) => actor ! alert
      case _ =>
    }
  }

  override def preStart() {
    log.debug("Starting analyser for events with name: " + name)
	    
	  /**
	   * Initialize analyser with activities from db?
	   */
  }

  override def postStop() {
    log.debug("Stopping analyser for events with name: " + name)
  }
}