package org.evactor.process.produce

import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.actor.ActorSystem
import akka.actor.Props
import akka.camel.CamelMessage
import akka.camel.Consumer
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.camel.CamelExtension
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.DataEvent
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class CamelProducerSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec {

  def this() = this(ActorSystem("CamelProducerSpec"))

  "Camel producer" must {

    "send events in json format to a camel endpoint" in {
      
      val probe = TestProbe()
      
      val consumer = system.actorOf(Props(new Consumer {
        def endpointUri = "seda:test"
         
        def receive = {
          case msg: CamelMessage ⇒ probe.ref ! msg.bodyAs[String]
          case msg => 
        }
      }))
      
      val producer = system.actorOf(Props(new CamelProducer(Nil, "seda:test")))
      Thread.sleep(1000)
      producer ! new AlertEvent("uuid", 0L, Set(), true, new DataEvent("uuid", 0L, "a"))
      
      
      probe.expectMsg(1 seconds, """{"id":"uuid","timestamp":0,"categories":[],"triggered":true,"event":{"id":"uuid","timestamp":0,"message":"a"}}""")
    }
    
  }

}