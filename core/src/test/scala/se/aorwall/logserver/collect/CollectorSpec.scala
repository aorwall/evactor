package se.aorwall.logserver.collect

import akka.actor.Actor
import Actor._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers

class CollectorSpec extends WordSpec with MustMatchers {
   "A Collector" must {

     "create a log event when receiving a log object and send to process actors" in {
         (pending)
     }

   }
}