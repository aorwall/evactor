package se.aorwall.logserver.monitor.statement

import scala.Predef._
import grizzled.slf4j.Logging
import akka.actor.{Actor, ActorRef}
import se.aorwall.logserver.model.Activity

abstract class StatementAnalyser (processId: String, alerter: ActorRef) extends Actor with Logging {  // get processid, statement, alert actor?

  self.id = processId
  private var triggered = false

  def receive = {
    case activity: Activity => analyse(activity)
  }

  /**
   * Initialize analyser with activities from db?
   */
  //def init(): Unit

  def analyse(activity: Activity): Unit

  def alert(message: String) = if (!triggered) {
    warn("Alert: " + message)

    alerter ! message

    triggered = true
  }

  def backToNormal(message: String) = if (triggered) {
    info("Back to normal: "  + message)

    alerter ! message

    triggered = false
  }
}