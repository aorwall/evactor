package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State

/**
 * Represents a simple log event from a component
 */
case class LogEvent(
    override val channel: String, 
    override val category: Option[String],
    override val id: String,
    override val timestamp: Long,
    val correlationId: String,
    val client: String,
    val server: String,
    val state: State,
    val message: String) 
  extends Event(channel, category, id, timestamp) 
  with HasMessage 
  with HasState {

  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new LogEvent(newChannel, newCategory, id, timestamp, correlationId, client, server, state, message)
  
}