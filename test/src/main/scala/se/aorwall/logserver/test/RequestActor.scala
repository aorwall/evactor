package se.aorwall.logserver.test

import util.Random
import akka.actor.{Actor, ActorRef}
import grizzled.slf4j.Logging
import java.util.UUID
import se.aorwall.logserver.model.{State, Log}

/**
 * Actor generating request for a specific business process
 */

class RequestActor(components: List[String], logReceiver: ActorRef) extends Actor with Logging {

  def receive = {
    case _ => sendRequests()
  }

  def sendRequests(): Unit = {
    var state = State.SUCCESS
    val correlationId = UUID.randomUUID().toString

    for (component <- components) if (state != State.INTERNAL_FAILURE) {
      val start = new Log("server", component, correlationId, "client", System.currentTimeMillis, State.START, "")
      debug("send: " + start)
      logReceiver ! start
      state = if (Random.nextInt(5) > 3) {
        State.INTERNAL_FAILURE
      }
      else State.SUCCESS
      Thread.sleep(Random.nextInt(3) + 1L)
      val stop = new Log("server", component, correlationId, "client", System.currentTimeMillis, state, "")
      debug("send: " + stop)
      logReceiver ! stop
      Thread.sleep(Random.nextInt(3) + 1L)

      if (state == State.INTERNAL_FAILURE) info("Fail: " + correlationId + ": " + component)

    }
  }

}