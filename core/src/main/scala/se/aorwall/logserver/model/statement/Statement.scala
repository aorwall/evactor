package se.aorwall.logserver.model.statement

import se.aorwall.logserver.model.{Alert, Activity}
import akka.actor.{ActorRef, Actor}

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
trait Statement {

  def createActor(processId: String): ActorRef


}