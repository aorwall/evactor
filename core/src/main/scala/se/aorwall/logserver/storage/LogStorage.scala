package se.aorwall.logserver.storage

import se.aorwall.logserver.model.{Statistics, Activity, Log}

/**
 * Interface for log storage
 *
 */

trait LogStorage {

  /**
   * Store a log object to specified activity
   */
  def storeLog(activityId: String, log: Log)

  /**
   * Read all log objects for a specified activity
   */
  def readLogs(activityId: String): List[Log]

  /**
   * Remove an activity and it's log objects
   */
  def removeActivity(activityId: String)

  /**
   * Store an activity object
   */
  def storeActivity(activity: Activity)

  /**
   * Read activity objects
   */
  def readActivities(processId: String, fromTimestamp: Option[Long], count: Int ): List[Activity]

  /**
   * Check if an activity exists
   */
  def activityExists(processId: String, activityId: String): Boolean

  /**
   * Read statistics for a process
   */
  def readStatistics(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long]): Statistics

}