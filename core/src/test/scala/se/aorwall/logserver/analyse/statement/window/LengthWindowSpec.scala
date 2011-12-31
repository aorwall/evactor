package se.aorwall.logserver.analyse.statement.window

import collection.immutable.TreeMap
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class LengthWindowSpec extends WordSpec with MustMatchers {

  val lengthWindow = new {
        type T = Int
        val noOfRequests = 3
      } with LengthWindow

  "A LengthWindowConf" must {

    "return inactive activites if there are more activities than the specified length" in {
      val activities = TreeMap(1L -> 11, 2L -> 22, 3L -> 33, 4L -> 44, 5L -> 55)
      lengthWindow.getInactive(activities) must be(Map(1L -> 11, 2L -> 22))
    }

    "don't return any activites if there are less activities than the specified length" in {
      val activities = TreeMap(1L -> 11, 2L -> 22)
      lengthWindow.getInactive(activities).size must be(0)
    }

  }
}