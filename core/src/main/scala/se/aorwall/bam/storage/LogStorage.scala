package se.aorwall.bam.storage

import se.aorwall.bam.model.{Statistics, Activity, Log}

/**
 * Interface for log storage
 *
 */

trait LogStorage {

  /**
   * Store a log object
   */
  def storeLog(correlationId: String, log: Log)

  /**
   * Read all log objects for a specified correlation id
   */
  def readLogs(correlationId: String): List[Log]

  /**
   * Start new active activity
   */
  def startActivity(activity: Activity)

  /**
   * Finish the activity
   */
  def finishActivity(activity: Activity)

  /**
   * Read unfinished activites
   */
  def readUnfinishedActivities(processId: String)

  /**
   * Read activity objects
   */
  def readActivities(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Activity]

  /**
   * Check if an activity exists
   */
  def activityExists(processId: String, activityId: String): Boolean

  /**
   * Read statistics for a process
   */
  def readStatistics(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long]): Statistics

}