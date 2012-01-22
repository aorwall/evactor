package se.aorwall.bam.process.analyse.window

import collection.immutable.TreeMap
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LengthWindowSpec extends WordSpec with MustMatchers {

  val lengthWindow = new {
        type S = Int
        val noOfRequests = 3
      } with LengthWindow

  "A LengthWindowConf" must {

    "return inactive events if there are more events than the specified length" in {
      val events = TreeMap(1L -> 11, 2L -> 22, 3L -> 33, 4L -> 44, 5L -> 55)
      lengthWindow.getInactive(events) must be(Map(1L -> 11, 2L -> 22))
    }

    "don't return any events if there are less events than the specified length" in {
      val events = TreeMap(1L -> 11, 2L -> 22)
      lengthWindow.getInactive(events).size must be(0)
    }

  }
}