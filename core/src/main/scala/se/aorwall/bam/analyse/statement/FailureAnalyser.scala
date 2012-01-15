package se.aorwall.bam.analyse.statement

import grizzled.slf4j.Logging
import akka.actor.{ActorRef}
import collection.immutable.TreeMap
import window.Window
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.State

class FailureAnalyser (val eventName: String, maxOccurrences: Long)
  extends StatementAnalyser with Window with Logging {

  type T = Event with HasState
  type S = Int

  var failedEvents = new TreeMap[Long, Int] ()

  def analyse(event: T) {

    if(event.state == State.FAILURE){ // check if event has state FAILURE

      // Add new
      failedEvents += (event.timestamp -> event.state)  // TODO: What if two activites have the same timestamp?

      // Remove old
      val inactiveEvents = getInactive(failedEvents)

      failedEvents = failedEvents.drop(inactiveEvents.size)

      trace(context.self + " failedEvents: " + failedEvents)
      debug(context.self + " no of failed events with name " + eventName + ": " + failedEvents.size)

      if(failedEvents.size > maxOccurrences) {
        alert(failedEvents.size + " failed events with name " + eventName + " is more than allowed (" + maxOccurrences + ")")
      } else {
        backToNormal("Back to normal!")
      }

    }
  }
}