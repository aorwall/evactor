package se.aorwall.bam.analyse.statement

import scala.Predef._
import grizzled.slf4j.Logging
import akka.actor.{Actor, ActorRef}
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.Alert

abstract class StatementAnalyser(processId: String) extends Actor with Logging {

  var triggered = false //TODO: use FSM for this
  var testAlerter: Option[ActorRef] = None

  def receive = {
    case event: Event => analyse(event)
    case testActor: ActorRef => testAlerter = Some(testActor)
  }

  /**
   * Initialize analyser with activities from db?
   */
  //def init(): Unit

  def analyse(event: Event)

  def alert(message: String) {
    if (!triggered) {
      warn("Alert: " + message)

      triggered = true
      sendAlert(message)
    }
  }

  def backToNormal(message: String) {
    if (triggered) {
      info("Back to normal: " + message)

      triggered = false
      sendAlert(message)
    }
  }

  def sendAlert(message: String) {
    val alert = new Alert(processId, message, triggered)

    context.actorSelection("../../alerter/" + processId)  ! alert //TODO: The alerter isn't implemented yet

    // If a test actor exists
    testAlerter match {
      case Some(testActor) => testActor ! alert
      case _ =>
    }
  }

  override def preStart() {
    trace("Starting statement analyser["+context.self+"]  for process " + processId)
  }

  override def postStop() {
    trace("Stopping statement analyser["+context.self+"] for process " + processId)
  }
}