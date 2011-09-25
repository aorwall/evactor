package se.aorwall.logserver

import akka.actor.Actor
import Actor._
import akka.event.EventHandler
import akka.camel.{CamelContextManager, CamelServiceManager, Message, Consumer}
import model.{State, Log}

object Test {
  def main(args: Array[String]): Unit = {
    val consumer = actorOf(new Actor with Consumer {
      def endpointUri = "direct:test"

      def receive = {
        case msg: Message => println(msg.bodyAs[String])
      }
    })

    val service = CamelServiceManager.startCamelService

    service.awaitEndpointActivation(1) {
      consumer.start()
    }

    CamelContextManager.mandatoryTemplate.requestBody("direct:test", "testing")

    service.awaitEndpointDeactivation(1) {
      consumer.stop()
    }

    service.stop
    EventHandler.shutdown()

  }
}