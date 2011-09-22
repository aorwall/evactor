package se.aorwall.logserver.model.process

import collection.immutable.Map
import se.aorwall.logserver.model.{Log, Activity}

/**
 *
 */
trait BusinessProcess {

  val processId: String

  /**
   * Check if business process contains a specific component
   */
  def contains(componentId: String): Boolean

  /**
   * Generate a unique activity id based on the log event
   */
  def getActivityId(logevent: Log): String

  /**
   * Return a new instance of the Business Processes ActivityBuilder
   */
  def getActivityBuilder(): ActivityBuilder

  /**
   * Defines if a new activity should be started
   */
  def startNewActivity(logevent: Log): Boolean

}

abstract class ActivityBuilder {

  /**
   * Add log event to activity builder
   */
  def addLogEvent(logevent: Log)

  /**
   * Check if the activity is finished
   */
  def isFinished(): Boolean

  /**
   * Create activity with current state
   */
  def createActivity(): Activity
}