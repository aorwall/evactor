package se.aorwall.bam.process

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProcessorSpec extends WordSpec with MustMatchers {

  "A Processor" must {

    "send log event to already running activity" in {
       (pending)
    }

    "start new running activity" in {
       (pending)
    }

  }
}