package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.State

trait RequestEventStorage extends EventStorage{

  def getLatencyStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Long)])
  
  def getEvents(channel: String, category: Option[String], state: Option[State], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event]
  
  def getStatistics(name: String, category: Option[String], state: Option[State], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long])
  
}

