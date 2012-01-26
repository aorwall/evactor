package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasMessage

/**
 * An event with a message of some sort
 */
case class DataEvent (
    override val name: String, 
    override val id: String, 
    override val timestamp: Long, 
    val message: String) extends Event(name, id, timestamp) with HasMessage  {

  override def clone(newName: String): Event = {
    new DataEvent(newName, id, timestamp, message)
  }
  
}
