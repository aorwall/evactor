package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

trait KpiEventStorage extends EventStorage{

  def readSumStatistics(name: String, fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[(Long, Double)])
  
}

