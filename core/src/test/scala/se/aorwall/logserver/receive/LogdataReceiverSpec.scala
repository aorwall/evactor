package se.aorwall.logserver.receive

import akka.actor.Actor
import Actor._
import org.scalatest.{WordSpec, FunSuite}
import org.scalatest.matchers.MustMatchers

class LogdataReceiverSpec extends WordSpec with MustMatchers {
   "A LogdataReceiver" must {

     "create a log event when receiving a log object and send to process actors" in {
         (pending)
     }

   }
}