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

/**
 * Need support for wildcards in eventName.
 * 
 * if eventName = type/name/* and name = test and the new eventname should be
 * the event.name + /name"... Doesn't work for AbsenceOfRequestsAnalyser though...
 */*/
abstract class Analyser(name: String, val eventName: Option[String]) extends Processor(name) with Publisher with ActorLogging  {
    
  var triggered = false //TODO: use FSM for this
  
  protected def alert(eventName: String, message: String) {
    if (!triggered) {
      log.warning("Alert: " + message)

      triggered = true
      sendAlert(eventName, message)
    }
  }

  protected def backToNormal(eventName: String, message: String) {
    if (triggered) {
      log.info("Back to normal: " + message)

      triggered = false
      sendAlert(eventName, message)
    }
  }

  def sendAlert(eventName: String, message: String) {
    val currentTime = System.currentTimeMillis
    val alert = new AlertEvent("%s/%s".format(eventName, name), currentTime.toString, currentTime, triggered, message)
    log.info(alert.path)
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