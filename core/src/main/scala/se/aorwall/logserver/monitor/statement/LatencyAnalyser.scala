package se.aorwall.logserver.monitor.statement

import window.Window
import collection.immutable.TreeMap
import se.aorwall.logserver.model.Activity
import grizzled.slf4j.Logging
import akka.actor.{ActorRef, Actor}

class LatencyAnalyser (processId: String, alerter: ActorRef, maxLatency: Long)
  extends StatementAnalyser (processId, alerter) with Window with Logging {

  type T = Long

  var activities = new TreeMap[Long, Long] ()
  var sum = 0L

  def analyse(activity: Activity) = if(activity.state == 10){ // check if activity has state SUCCESS (10)

    // Add new
    val latency = activity.endTimestamp - activity.startTimestamp
    activities += (activity.startTimestamp -> latency)  // TODO: Use endTimestamp instead of startTimestamp?
    sum += latency

    // Remove old
    val inactiveActivites = getInactive(activities)
    activities = activities.drop(inactiveActivites.size)
    sum += inactiveActivites.foldLeft(0L){ case (a, (k, v)) => a-v }

    // Count average latency
    val avgLatency = if(sum > 0){
      sum / activities.size
    } else {
      0
    }

    trace(activities)
    debug("sum: " + sum + ", no of activities: " + activities.size + ", avgLatency: " + avgLatency)

    if(avgLatency > maxLatency) {
      alert("Average latency " + avgLatency + "ms is higher than the maximum allowed latency " + maxLatency + "ms")
    } else {
      backToNormal("back to normal!")
    }

  }

}