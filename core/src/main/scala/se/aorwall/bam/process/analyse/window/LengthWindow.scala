package se.aorwall.bam.process.analyse.window

import grizzled.slf4j.Logging

/**
 *
 */
trait LengthWindow extends Window with Logging {

  val noOfRequests: Int

  override def getInactive(activities: Map[Long, S]): Map[Long, S] = {

    val noOfInactive = activities.size - noOfRequests

    if(noOfInactive > 0)
      activities.take(noOfInactive)
    else
      Map()

  }
}

case class LengthWindowConf (noOfRequests: Int) extends WindowConf {

}