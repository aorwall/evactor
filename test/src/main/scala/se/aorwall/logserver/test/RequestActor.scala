package se.aorwall.logserver.test

import se.aorwall.logserver.model.{State, Log}
import util.Random
import akka.actor.{Actor, ActorRef}
import grizzled.slf4j.Logging

/**
 * Actor generating request for a specific business process
 */

class RequestActor (components: List[String], logReceiver: ActorRef) extends Actor with Logging {

  def receive = {
    case _ => sendRequests()
  }

  def sendRequests(): Unit = {
    var failed = false
    val correlationId = System.currentTimeMillis()+""

     for (component <- components ; if !failed) {

        debug(logReceiver.isRunning)

        logReceiver ! new Log("server", component, correlationId, "client", System.currentTimeMillis, State.START, "")
        failed = Random.nextBoolean
        val state = if(failed) Random.nextInt(3)+11
                    else State.SUCCESS
        Thread.sleep(Random.nextInt(300)+5L)

        debug(logReceiver.isRunning)
        logReceiver ! new Log("server", component, correlationId, "client", System.currentTimeMillis, state, "")
        Thread.sleep(Random.nextInt(300)+5L)
     }
  }
}