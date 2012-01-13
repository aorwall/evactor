package se.aorwall.bam.analyse.statement.window

import grizzled.slf4j.Logging

/**
 *
 */
trait LengthWindow extends Window with Logging {

  val noOfRequests: Int

  override def getInactive(activities: Map[Long, T]): Map[Long, T] = {

    val noOfInactive = activities.size - noOfRequests

    if(noOfInactive > 0)
      activities.take(noOfInactive)
    else
      Map()

  }
}