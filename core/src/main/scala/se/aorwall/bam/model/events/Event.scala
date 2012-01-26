package se.aorwall.bam.model.events

import scala.Array

/**
 * The event class all other event's should inherit
 */

class Event (
    val name: String,
    val id: String,
    val timestamp: Long) extends Serializable {
  
  /**
   * Clone the event but change the event name. Need to create 
   * new events with changed names
   */
  def clone(newName: String): Event = {
    new Event(newName, id, timestamp)
  }
}

object EventRef {
  
  def apply(event: Event) = new EventRef(event.getClass.getName, event.name, event.id)
  
  def fromString(string: String): EventRef = {
    string.split("/") match {
      case Array(className, name, id) => new EventRef(className, name, id)
      case _ => throw new IllegalArgumentException("Couldn't create an EventRef instance from argument: " + string)
    }
  }  
  
}

case class EventRef (
    val className: String,
    val name: String,
    val id: String) {
  
  override def toString() = "%s/%s/%s".format(className, name, id)
    
}