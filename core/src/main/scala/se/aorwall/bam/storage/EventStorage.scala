package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.State

trait EventStorage {
  
  def storeEvent(event: Event): Unit
  
  def getEvent(id: String): Option[Event]
  
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]
  
  def getStatistics(name: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long])
  
  def eventExists(event: Event): Boolean
  
  def getEventChannels(count: Int): List[(String, Long)]
  
  def getEventCategories(channel: String, count: Int): List[(String, Long)]
}
