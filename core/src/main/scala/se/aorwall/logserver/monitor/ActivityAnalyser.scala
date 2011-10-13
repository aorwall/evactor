package se.aorwall.logserver.monitor

import akka.actor.{Actor}
import Actor._
import se.aorwall.logserver.model.{Activity}
import grizzled.slf4j.Logging
import akka.routing._

/**
 * 1. Receive finished activity
 * 2. Send to analysers
 */
class ActivityAnalyser extends Actor with Logging {

  def receive = {
    case activity: Activity => analyse(activity)
  }

  /**
   * Send activity to all active statement analysers for the business process
   */
  def analyse(activity: Activity) {

    val statementAnalysers = Actor.registry.actorsFor(activity.processId)
    info("found " + statementAnalysers.size + " statement monitors")

    for(statementAnalyser <- statementAnalysers) statementAnalyser ! activity

  }

  override def preStart() {
    trace("Starting Activity monitor with id " + self.id)
  }

  override def postStop() {
    trace("Stopping Activity monitor with id " + self.id)
  }


}

class ActivityAnalyserPool extends Actor with DefaultActorPool
                               with FixedSizeCapacitor
                               with SmallestMailboxSelector
{
   def receive = _route()
   def limit = 5
   def partialFill = true
   def selectionCount = 1
   def instance() = actorOf[ActivityAnalyser]
}