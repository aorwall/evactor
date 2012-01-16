package se.aorwall.bam.model.events

/**
 * The event class all other event's should inherit
 */

class Event (
    val name: String,
    val id: String,
    val timestamp: Long) extends Serializable {
}
