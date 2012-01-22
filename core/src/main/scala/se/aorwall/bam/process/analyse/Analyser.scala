package se.aorwall.bam.process.analyse

import scala.Predef._
import grizzled.slf4j.Logging
import akka.actor.{Actor, ActorRef}
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Processor
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.process.CheckEventName

abstract class Analyser(name: String, val eventName: Option[String]) extends Processor(name) with CheckEventName with Logging  {
    
  var triggered = false //TODO: use FSM for this
  
  protected def alert(message: String) {
    if (!triggered) {
      warn(context.self + " Alert: " + message)

      triggered = true
      sendAlert(message)
    }
  }

  protected def backToNormal(message: String) {
    if (triggered) {
      info(context.self + " Back to normal: " + message)

      triggered = false
      sendAlert(message)
    }
  }

  def sendAlert(message: String) {
    val currentTime = System.currentTimeMillis
    val alert = new AlertEvent(name, currentTime.toString, currentTime, triggered, message)
    
    collector  ! alert //TODO: The alerter isn't implemented yet

    // If a test actor exists
    testActor match {
      case Some(actor) => actor ! alert
      case _ =>
    }
  }

  override def preStart() {
    trace(context.self + " Starting analyser for events with name: " + name)
	    
	  /**
	   * Initialize analyser with activities from db?
	   */
  }

  override def postStop() {
    trace(context.self + " Stopping analyser for events with name: " + name)
  }
}