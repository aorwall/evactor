package se.aorwall.bam.analyse.statement

import grizzled.slf4j.Logging
import akka.actor.{ActorRef}
import collection.immutable.TreeMap
import window.Window
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event

class FailureAnalyser (processId: String, states: List[Int], maxOccurrences: Long)
  extends StatementAnalyser (processId) with Window with Logging {

  type T = Int

  var failedActivities = new TreeMap[Long, Int] ()

  def analyse(event: Event) {

  /*  if(states.contains(event.state)){ // check if activity has a failure state  TODO: state in states?

      // Add new
      failedActivities += (activity.startTimestamp -> activity.state)  // TODO: Use endTimestamp instead of startTimestamp? And what if two activites have the same timestamp?

      // Remove old
      val inactiveActivites = getInactive(failedActivities)

      failedActivities = failedActivities.drop(inactiveActivites.size)

      trace(failedActivities)
      debug("no of activities for process " + processId + " with state " + states + ": " + failedActivities.size)

      if(failedActivities.size > maxOccurrences) {
        alert(failedActivities.size + " failed activites in process " + processId + " with state " + states + " is more than allowed (" + maxOccurrences + ")")
      } else {
        backToNormal("back to normal!")
      }

    }*/
  }
}