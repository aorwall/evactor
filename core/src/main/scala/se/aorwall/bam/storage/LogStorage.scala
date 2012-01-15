package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

/**
 * Interface for log storage
 *
 */
trait LogStorage {

  /**
   * Store an event
   */
  def storeEvent(event: Event)

  /**
   * Read an event
   */
  def readEvent(id: String): Event

  /**
   * Start new active activity
   */
  def startActivity(event: Event)

  /**
   * Finish the activity
   */
  def finishEvent(event: Event)

  /**
   * Read unfinished activites
   */
  def readUnfinishedActivities(processId: String)

  /**
   * Read activity objects
   */
  def readEvents(name: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]

  /**
   * Check if an activity exists
   */
  def eventExists(processId: String, activityId: String): Boolean

  /**
   * Read statistics for a process
   */
  def readStatistics(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long]): Map[String, Long]

}