package se.aorwall.logserver.storage

import se.aorwall.logserver.model.{Activity, Log}

/**
 * Interface for log storage
 *
 */

trait LogStorage {

  /**
   * Store a log object to specified activity
   */
  def storeLog(activityId: String, log: Log): Unit

  /**
   * Read all log objects for a specified activity
   */
  def readLogs(activityId: String): List[Log]

  /**
   * Remove an activity and it's log objects
   */
  def removeActivity(activityId: String): Unit

  /**
   * Store an activity object
   */
  def storeActivity(activity: Activity): Unit

  /**
   * Read activity objects
   */
  def readActivities(processId: String, fromTimestamp: Option[Long], count: Int ): List[Activity]

  /**
   * Check if an activity exists
   */
  def activityExists(processId: String, activityId: String): Boolean

}