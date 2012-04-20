package se.aorwall.bam.process.extract

import se.aorwall.bam.model.events.Event

/**
 * Creating new event and adds to new channel
 */
trait EventCreator {

  def createBean(value: Option[Any], event: Event, newChannel: String): Option[Event]
  
}