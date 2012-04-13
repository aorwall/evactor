package se.aorwall.bam.model.events

/**
 * An event with a measurable value
 */
case class KpiEvent (
    override val channel: String, 
    override val category: Option[String],
    override val id: String, 
    override val timestamp: Long, 
    val value: Double) 
  extends Event(channel, category, id, timestamp)  {

  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new KpiEvent(newChannel, newCategory, id, timestamp, value)
  
}
