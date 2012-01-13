package se.aorwall.bam.model.process

import se.aorwall.bam.model.{Log, Activity}

/**
 * Represents a business process from which activities
 */
trait BusinessProcess {

  val processId: String
  val timeout: Long // Timeout set in seconds

  /**
   * Check if the business process
   */
  def handlesEvent(event: Log): Boolean
  
  /**
   * Return activity id for this event
   */
  def getActivityId(event: Log): String
  
  /**
   * Return a new instance of the Business Process' ActivityBuilder
   */
  def getActivityBuilder: ActivityBuilder

}

/**
 * Represents the builder of the activity that decides when the activity is finished. Implementations of this trait
 * should be idempotent to handle duplicate log events.
 */
trait ActivityBuilder {

  /**
   * Add log event to activity builder
   */
  def addLogEvent(logevent: Log)

  /**
   * Check if the activity is finished
   */
  def isFinished: Boolean

  /**
   * Create activity with current state
   */
  def createActivity(): Activity

  /**
   * Clear state of activity builder
   */
  def clear()
}