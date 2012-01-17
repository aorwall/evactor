package se.aorwall.bam.model.events

import se.aorwall.bam.model.attributes.HasLatency
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.State
import se.aorwall.bam.model.attributes.HasMessage

/**
 * An event representing a keyword
 */
case class KeywordEvent (
    override val name: String, 
    override val id: String, 
    override val timestamp: Long, 
    val keyword: String,
    val eventRef: Event) extends Event(name, id, timestamp) {

}