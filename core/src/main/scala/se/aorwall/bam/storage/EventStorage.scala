package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

trait EventStorage {
  
  def storeEvent(event: Event): Boolean
  def readEvents(eventName: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]
  
}