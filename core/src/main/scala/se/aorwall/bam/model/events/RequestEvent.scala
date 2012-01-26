package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State

/**
 * Represents a completed request to a component 
 */
case class RequestEvent (
    override val name: String, 
    override val id: String, 
    override val timestamp: Long, 
    val inboundRef: Option[EventRef],
    val outboundRef: Option[EventRef],
    val state: State,
    val latency: Long) extends Event(name, id, timestamp) with HasLatency with HasState  {

  override def clone(newName: String): Event = {
    new RequestEvent(newName, id, timestamp, inboundRef, outboundRef, state, latency)
  }
}