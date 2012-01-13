package se.aorwall.bam.model

/**
 * An Activity represents a finished business activity
 */

case class Activity (processId: String,
                activityId: String,
                state: Int,
                startTimestamp: Long,
                endTimestamp: Long) {

}