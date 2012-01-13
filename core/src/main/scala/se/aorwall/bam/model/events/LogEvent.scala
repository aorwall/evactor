package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State

/**
 * Represents a simple log event from a component
 */
case class LogEvent(
    override val name: String,
    override val id: String,
    override val timestamp: Long,
    val correlationId: String,
    val client: String,
    val server: String,
    val state: Integer,
    val message: String) extends Event(name, id, timestamp) with HasMessage with HasState {

}