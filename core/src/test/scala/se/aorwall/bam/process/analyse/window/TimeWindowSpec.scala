package se.aorwall.bam.process.analyse.window

import collection.immutable.TreeMap
import grizzled.slf4j.Logging
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TimeWindowSpec extends WordSpec with MustMatchers with Logging {

  val currentTime = System.currentTimeMillis
  val events = TreeMap(
      currentTime - 10000000L -> 11, 
      currentTime - 1000000L -> 22, 
      currentTime - 1000L -> 33, 
      currentTime - 2000L -> 44, 
      currentTime - 3000L -> 55)

  "A TimeWindow" must {
/*
    "return timed out events" in {

      new {
        type S = Int
        val timeframe = 10000L
      } with TimeWindow {
        assert(getInactive(events) == Map(currentTime - 10000000L -> 11, currentTime - 1000000L -> 22))
      }
    }
*/
    "not return any events when the timeframe is set to current time" in {

      new {
        type S = Int
        val timeframe = System.currentTimeMillis
      } with TimeWindow {
        assert(getInactive(events).size == 0)
      }
    }

    "return all events when the timelimit is set to 0" in {

      new {
        type S = Int
        val timeframe = 0L
      } with TimeWindow {
        assert(getInactive(events).size == 5)
      }
    }
  }
}