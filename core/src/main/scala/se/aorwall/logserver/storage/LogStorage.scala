package se.aorwall.logserver.storage

import se.aorwall.logserver.model.{Activity, Log, LogEvent}
import collection.mutable.Map

/**
 * Interface for log storage
 *
 */

trait LogStorage {

  def storeLogdata(logdata: Log): Unit

  def storeLogEvent(processId: String, logevent: LogEvent): Unit

  def readLogEvents(processId: String, correlationId: String): Map[String, List[Int]]

  def removeLogEvents(processId: String, correlationId: String): Unit

  def storeActivity(activity: Activity)
}