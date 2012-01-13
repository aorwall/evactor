package se.aorwall.bam.model.attributes

import se.aorwall.bam.model.State

trait HasState {
	val state: Integer // TODO: Should be some kind of enum!
}