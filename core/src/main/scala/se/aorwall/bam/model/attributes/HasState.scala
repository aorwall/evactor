package se.aorwall.bam.model.attributes

import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.Event

trait HasState {
  val state: State
}