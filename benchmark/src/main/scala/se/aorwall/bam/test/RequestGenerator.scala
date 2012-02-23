package se.aorwall.bam.test

import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import scala.util.Random
import java.util.UUID
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorLogging
import se.aorwall.bam.model._
import se.aorwall.bam.model.events.LogEvent

class RequestGenerator (val businessProcesses: Seq[SimpleProcess], val noOfRequests: Int = 100000, val collector: ActorRef) extends Actor {
  
  val requestActorList = businessProcesses map { process => context.actorOf(Props(new RequestActor(process, collector)), name = "request"+process.name) }  
  
  def receive = {
    case _ => for(x <- 0 until noOfRequests) requestActorList.foreach(_ ! None)
  }
  
}

class RequestActor(process: SimpleProcess, collector: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case _ => sendRequests()
  }

  def sendRequests() {
    val correlationId = UUID.randomUUID().toString

    for (component <- process.components) {
      val start = new LogEvent(component, correlationId, System.currentTimeMillis, correlationId, "client", "server", Start, "hello")
      log.debug("send: " + start)
      collector ! start
      val state: State = if (Random.nextInt(5) > 3) Failure else Success

      Thread.sleep(Random.nextInt(3) + 1L)
      val stop = new LogEvent(component, correlationId, System.currentTimeMillis, correlationId, "client", "server", state, "goodbye")
      			  
      log.debug("send: " + stop)
      collector ! stop
      Thread.sleep(Random.nextInt(3) + 1L)
      
      // abort requester on failure
      state match {
        case Failure => return
        case _ => if (Random.nextInt(25) == 1) return   //TIMEOUT on one of 25
      }      
    }
  }
}