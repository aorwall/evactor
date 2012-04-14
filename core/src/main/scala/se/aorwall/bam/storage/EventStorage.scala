package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

trait EventStorage {
  
  def storeEvent(event: Event): Unit
  
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]
  
  def getStatistics(name: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long])
  
  def eventExists(event: Event): Boolean
  
  def getEventCategories(channel: String, count: Int): Map[String, Long]
}
