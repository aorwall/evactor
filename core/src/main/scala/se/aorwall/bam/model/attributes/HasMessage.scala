package se.aorwall.bam.model.attributes
import se.aorwall.bam.model.events.Event

/**
 * Objects with this traits includes a message
 * 
 */
trait HasMessage {
	val message: String
}