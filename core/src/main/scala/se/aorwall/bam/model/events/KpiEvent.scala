package se.aorwall.bam.model.events

/**
 * An event with a measurable value
 */
case class KpiEvent (
    override val name: String, 
    override val id: String, 
    override val timestamp: Long, 
    val value: Double) 
  extends Event(name, id, timestamp)  {

  override def clone(newName: String): Event = 
    new KpiEvent(newName, id, timestamp, value)
  
}
