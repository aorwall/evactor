package se.aorwall.bam.process.build

import se.aorwall.bam.model.events.Event
import akka.actor.Actor

/**
 * Represents the builder of the event that decides when the event is ready to be created. Implementations of this trait
 * should be idempotent to handle duplicate incoming events.
 */
trait EventBuilder extends Actor {
    
  /**
   * Add event to event builder
   */
  def addEvent(event: Event)

  /**
   * Check if the event is finished
   */
  def isFinished: Boolean

  /**
   * Create activity with current state
   */
  def createEvent(): Event 

  /**
   * Clear state of activity builder
   */
  def clear()
}