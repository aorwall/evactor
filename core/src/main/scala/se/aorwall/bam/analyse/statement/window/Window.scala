package se.aorwall.bam.analyse.statement.window

/**
 * Specifies a window with activities, returns activities outside the window
 */
trait Window {
  type T

  def getInactive(activities: Map[Long, T]) = Map[Long, T]()
}