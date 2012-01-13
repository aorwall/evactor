package se.aorwall.bam.collect

import akka.actor.Actor
import Actor._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CollectorSpec extends WordSpec with MustMatchers {
   "A Collector" must {

     "create a log event when receiving a log object and send to process actors" in {
         (pending)
     }

   }
}