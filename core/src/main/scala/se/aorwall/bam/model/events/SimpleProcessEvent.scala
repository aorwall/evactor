package se.aorwall.bam.model.events
import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State

case class SimpleProcessEvent(
    override val channel: String, 
    override val category: Option[String],
    override val id: String, 
    override val timestamp: Long,
    val requests: List[EventRef], // eventRefs
    val state: State,    
    val latency: Long) 
  extends Event(channel, category, id, timestamp) 
  with HasLatency
  with HasState  {
  
  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new SimpleProcessEvent(newChannel, newCategory, id, timestamp, requests, state, latency)
  
}