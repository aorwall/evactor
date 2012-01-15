package se.aorwall.bam.analyse.statement

import scala.Predef._
import grizzled.slf4j.Logging
import akka.actor.{Actor, ActorRef}
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.Alert

abstract class StatementAnalyser extends Actor with Logging {

  val eventName: String
  
  type T <: Event
  
  var triggered = false //TODO: use FSM for this
  var testAlerter: Option[ActorRef] = None

  def receive = {
    case event: T if(event.name == eventName) => analyse(event)
    case testActor: ActorRef => testAlerter = Some(testActor)
    case _ => 
  }

  /**
   * Initialize analyser with activities from db?
   */
  //def init(): Unit

  def analyse(event: T)

  def alert(message: String) {
    if (!triggered) {
      warn(context.self + " Alert: " + message)

      triggered = true
      sendAlert(message)
    }
  }

  def backToNormal(message: String) {
    if (triggered) {
      info(context.self + " Back to normal: " + message)

      triggered = false
      sendAlert(message)
    }
  }

  def sendAlert(message: String) {
    val alert = new Alert(eventName, message, triggered)

    context.actorSelection("../../alerter/" + eventName)  ! alert //TODO: The alerter isn't implemented yet

    // If a test actor exists
    testAlerter match {
      case Some(testActor) => testActor ! alert
      case _ =>
    }
  }

  override def preStart() {
    trace(context.self + " Starting analyser for events with name: " + eventName)
  }

  override def postStop() {
    trace(context.self + " Stopping analyser for events with name: " + eventName)
  }
}