package se.aorwall.logserver.analyse.statement.window

import collection.immutable.TreeMap
import grizzled.slf4j.Logging
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers

class TimeWindowSpec extends WordSpec with MustMatchers with Logging {

  val currentTime = System.currentTimeMillis
  val activities = TreeMap(currentTime - 10000000L -> 11, currentTime - 1000000L -> 22, currentTime - 1000L -> 33, currentTime - 2000L -> 44, currentTime - 3000L -> 55)

  "A TimeWindow" must {

    "return timed out activities" in {

      new {
        type T = Int
        val timeframe = 10000L
      } with TimeWindow {
        assert(getInactive(activities) == Map(currentTime - 10000000L -> 11, currentTime - 1000000L -> 22))
      }
    }

    "don't return any activities when the timeframe is set to current time" in {

      new {
        type T = Int
        val timeframe = System.currentTimeMillis
      } with TimeWindow {
        assert(getInactive(activities).size == 0)
      }
    }

    "return all activities when the timelimit is set to 0" in {

      new {
        type T = Int
        val timeframe = 0L
      } with TimeWindow {
        assert(getInactive(activities).size == 5)
      }
    }
  }
}