package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.model.attributes.HasState

/**
 * Represents a completed request to a component 
 */
case class RequestEvent(
    override val name: String, 
    override val id: String, 
    override val timestamp: Long, 
    val inbound: Option[LogEvent],
    val outbound: Option[LogEvent],
    val state: Int,
    val latency: Long) extends Event(name, id, timestamp) with HasLatency with HasState  {

}