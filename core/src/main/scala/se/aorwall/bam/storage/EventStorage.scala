package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

trait EventStorage {
  
  def storeEvent(event: Event): Unit
  
  def getEvents(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]
  
  def getStatistics(name: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long])
  
  def eventExists(event: Event): Boolean
  
  def getEventNames(parent: Option[String], count: Int): Map[String, Long]
}
