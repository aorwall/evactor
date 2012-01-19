package se.aorwall.bam.model.events

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
