package se.aorwall.bam.test

import scala.util.Random
import java.util.UUID
import akka.actor.{ActorRef, Actor, Props, ActorLogging}
import se.aorwall.bam.model._
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess

class RequestGenerator (
    channels: List[String], 
    collector: ActorRef,
    counter: ActorRef,
    noOfRequests: Int = 1,
    timeBetweenProcesses: Int = 500,
    timeBetweenRequests: Int = 100)
  extends Actor {
  
  val requestActor = context.actorOf(Props(new RequestActor(channels, collector, counter, noOfRequests)).withDispatcher("pinned-dispatcher"), name = "request")   
  
  def receive = {
    case _ => requestActor ! None
  }
  
}

class RequestActor(
    channels: List[String], 
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
        log.debug("processing requests for " + channels)
        sendRequests() 
        counter ! 1
        randomSleep(timeBetweenProcesses)    
      }
    }
  }

  private[this] def sendRequests() {

    for (channel <- channels) {
    	val correlationId = UUID.randomUUID().toString
      val start = new LogEvent(channel, None, correlationId, System.currentTimeMillis, correlationId, "client", "server", Start, "hello")
      log.debug("send: " + start)
      collector ! start
      val state: State = if (oneIn(50) ) Failure else Success

      randomSleep(timeBetweenRequests)
      val stop = new LogEvent(channel, None, correlationId, System.currentTimeMillis, correlationId, "client", "server", state, "goodbye")
      			  
      log.debug("send: " + stop)
      collector ! stop
      
      randomSleep(timeBetweenRequests)
    }
  }
  
  private[this] def randomSleep (time: Int) {
    Thread.sleep(Random.nextInt(time) + 1L)
  }
  
  private[this] def oneIn (x: Int) = Random.nextInt(x) == 0
  
}