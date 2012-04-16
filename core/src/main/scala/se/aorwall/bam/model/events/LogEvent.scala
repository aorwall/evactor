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

  private val serialVersionUID = 0L

  // Java friendly constructor
  def this(channel: String, category: String, id: String, timestamp: Long, correlationId: String, client: String, server: String, state: String, message: String) = {
    this(channel, Some(category), id, timestamp, correlationId, client, server, State(state), message )
  }
  
  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new LogEvent(newChannel, newCategory, id, timestamp, correlationId, client, server, state, message)
  
}