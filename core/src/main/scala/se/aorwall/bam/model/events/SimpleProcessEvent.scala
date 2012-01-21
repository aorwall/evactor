package se.aorwall.bam.model.events
import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State

case class SimpleProcessEvent(
    override val name: String, 
    override val id: String, 
    override val timestamp: Long,
    val requests: List[EventRef], // eventRefs
    val state: State,    
    val latency: Long) extends Event(name, id, timestamp) with HasLatency with HasState  {
}