package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasMessage

/**
 * An event with a message of some sort
 */
case class DataEvent (    
    override val channel: String, 
    override val category: Option[String],
    override val id: String, 
    override val timestamp: Long, 
    val message: String) 
  extends Event(channel, category, id, timestamp) 
  with HasMessage {

  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new DataEvent(newChannel, newCategory, id, timestamp, message)
  
}