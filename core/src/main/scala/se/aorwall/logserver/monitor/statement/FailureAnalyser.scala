package se.aorwall.logserver.monitor.statement

import se.aorwall.logserver.model.Activity
import grizzled.slf4j.Logging
import akka.actor.{ActorRef}
import collection.immutable.TreeMap
import window.Window

class FailureAnalyser (processId: String, alerter: ActorRef, states: List[Int], maxOccurrences: Long)
  extends StatementAnalyser (processId, alerter) with Window with Logging {

  type T = Int

  var failedActivities = new TreeMap[Long, Int] ()

  def analyse(activity: Activity) = if(states.contains(activity.state)){ // check if activity has a failure state  TODO: state in states?

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

  }
}