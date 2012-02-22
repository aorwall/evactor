package se.aorwall.bam.model.attributes

import se.aorwall.bam.model.events.Event

trait HasLatency {
  val latency: Long
}