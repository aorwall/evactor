package se.aorwall.bam.model.attributes

import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.Event

trait HasState {
	val state: Int // TODO: Should be some kind of enum or case class!
}