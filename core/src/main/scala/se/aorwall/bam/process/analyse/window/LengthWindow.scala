package se.aorwall.bam.process.analyse.window

import akka.actor.ActorLogging

/**
 *
 */
trait LengthWindow extends Window {

  val noOfRequests: Int

  override protected[analyse] def getInactive(activities: Map[Long, S]): Map[Long, S] = {

    val noOfInactive = activities.size - noOfRequests

    if(noOfInactive > 0)
      activities.take(noOfInactive)
    else
      Map()

  }
}

case class LengthWindowConf (noOfRequests: Int) extends WindowConf {

}