package se.aorwall.bam.model.events

case class AlertEvent (override val name: String, 
							  override val id: String, 
							  override val timestamp: Long,
							  val triggered: Boolean,
							  val message: String) extends Event(name, id, timestamp) {

}
