package se.aorwall.bam.process.analyse.window

/**
 * Specifies a window with activities, returns activities outside the window
 */
trait Window {
  type S

  def getInactive(activities: Map[Long, S]) = Map[Long, S]()
}

trait WindowConf {

}