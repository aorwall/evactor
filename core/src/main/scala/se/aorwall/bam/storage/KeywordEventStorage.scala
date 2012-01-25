package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event

trait KeywordEventStorage extends EventStorage {
  
  def getKeywords(eventName: String, count: Int, startsWith: Option[String]): List[String]

}

