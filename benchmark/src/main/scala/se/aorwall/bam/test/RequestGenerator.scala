package se.aorwall.bam.test

import scala.util.Random
import java.util.UUID
import akka.actor.{ActorRef, Actor, Props, ActorLogging}
import se.aorwall.bam.model._
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess

class RequestGenerator (
    businessProcesses: Seq[SimpleProcess], 
    collector: ActorRef, 
    counter: ActorRef,    
    noOfRequests: Int = 1,
    timeBetweenProcesses: Int = 500,
    timeBetweenRequests: Int = 100)
  extends Actor {
  
  val requestActorList = businessProcesses map { 
    process => 
      context.actorOf(Props(new RequestActor(process, collector, counter, noOfRequests)).withDispatcher("pinned-dispatcher"), name = "request"+process.name) }  
  
  def receive = {
    case _ => requestActorList.foreach(_ ! None)
  }
  
}

class RequestActor(
    process: SimpleProcess, 
    collector: ActorRef, 
    counter: ActorRef, 
    noOfRequests: Int = 1,
    timeBetweenProcesses: Int = 500,
    timeBetweenRequests: Int = 100) 
  extends Actor 
  with ActorLogging {

  def receive = {
    case _ => {
      for(x <- 0 until noOfRequests) {
        log.debug("processing requests for " + process.name)
        sendRequests() 
        counter ! 1
        randomSleep(timeBetweenProcesses)    
      }
    }
  }

  private[this] def sendRequests() {
    val correlationId = UUID.randomUUID().toString

    for (component <- process.components) {
      val start = new LogEvent(component, correlationId, System.currentTimeMillis, correlationId, "client", "server", Start, "hello")
      log.debug("send: " + start)
      collector ! start
      val state: State = if (oneIn(50) ) Failure else Success

      randomSleep(timeBetweenRequests)
      val stop = new LogEvent(component, correlationId, System.currentTimeMillis, correlationId, "client", "server", state, "goodbye")
      			  
      log.debug("send: " + stop)
      collector ! stop
      randomSleep(timeBetweenRequests)
            
      // abort requester on failure
      state match {
        case Failure => return
        case _ => if (oneIn(100)) return // TIMEOUT on one of 100 requests
      }    
    }
  }
  
  private[this] def randomSleep (time: Int) {
    Thread.sleep(Random.nextInt(time) + 1L)
  } 
  
  private[this] def oneIn (x: Int) = Random.nextInt(x) == 0
  
}