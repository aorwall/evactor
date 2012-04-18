package se.aorwall.bam.model.events

case class AlertEvent (
    override val channel: String, 
    override val category: Option[String],
    override val id: String, 
    override val timestamp: Long,
    val triggered: Boolean,
    val message: String,
    val eventRef: Option[EventRef]) 
  extends Event (channel, category, id, timestamp) {

  override def clone(newChannel: String, newCategory: Option[String]): Event = 
    new AlertEvent(newChannel, newCategory, id, timestamp, triggered, message, eventRef)
}
