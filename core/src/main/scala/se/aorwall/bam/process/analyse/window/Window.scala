package se.aorwall.bam.process.analyse.window

import se.aorwall.bam.process.analyse.Analyser

/**
 * Specifies a window with activities, returns activities outside the window
 */
trait Window {
  type S

  protected[analyse] def getInactive(activities: Map[Long, S]) = Map[Long, S]()
}

trait WindowConf {

}