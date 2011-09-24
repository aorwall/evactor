package se.aorwall.logserver.model.statement

import akka.actor.{Actor, ActorRef}
import Actor._
import se.aorwall.logserver.alert.Alerter

/**
 * A statement should:
 * 1. Hold configuration
 * 2. Create Actor
 * 3. Contain a logic to monitor activities against statement
 * 4. Handle the statements life cycle
 *
 * Maybe a Factory for each StatementAnalyser to publish the statement to the configuration
 * environment?
 */
abstract class Statement (val statementId: String, val alertEndpoint: String) {

  def createActor(processId: String): ActorRef

  def createAlerter() = actorOf(new Alerter(alertEndpoint))

}