package se.aorwall.bam.process
import se.aorwall.bam.model.events.Event

/**
 * Represents the builder of the event that decides when the event is ready to be created. Implementations of this trait
 * should be idempotent to handle duplicate incoming events.
 */
abstract class EventBuilder {
    
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