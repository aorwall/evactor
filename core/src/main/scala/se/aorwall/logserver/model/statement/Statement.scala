package se.aorwall.logserver.model.statement

import akka.actor.{Actor, ActorRef}
import Actor._
import se.aorwall.logserver.alert.Alerter

abstract class Statement (val processId: String, val statementId: String, val alertEndpoint: String) {

  val alerter = actorOf(new Alerter(alertEndpoint))
  val statementMonitor: ActorRef

  def startMonitor() {
    alerter.start()
    statementMonitor.start()
  }

  def stopMonitor() {
    statementMonitor.stop()
    alerter.stop()
  }

}