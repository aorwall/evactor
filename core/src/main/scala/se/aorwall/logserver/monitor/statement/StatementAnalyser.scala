package se.aorwall.logserver.monitor.statement

import scala.Predef._
import grizzled.slf4j.Logging
import akka.actor.{Actor, ActorRef}
import se.aorwall.logserver.model.{Alert, Activity}
import se.aorwall.logserver.storage.LogStorage
import akka.stm._

abstract class StatementAnalyser (processId: String, alerter: ActorRef) extends Actor with Logging {  // get processid, statement, alert actor?

  self.id = processId
  private val triggeredRef = Ref(false)

  def triggered = atomic {
     triggeredRef.get
  }

  def triggered(trig: Boolean) = atomic {
     triggeredRef.set(trig)
  }

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

    triggered(true)
    sendAlert(message)
  }

  def backToNormal(message: String) = if (triggered) {
    info("Back to normal: "  + message)

    triggered(false)
    sendAlert(message)
  }

  def sendAlert(message: String ){
   val alert = new Alert(processId, message, triggered)
   alerter ! alert
  }
}