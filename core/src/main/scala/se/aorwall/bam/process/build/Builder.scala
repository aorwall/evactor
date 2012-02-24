package se.aorwall.bam.process.build

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.{Processor, Monitored}

abstract class Builder (override val name: String) 
  extends Processor (name) 
  with Monitored
  with ActorLogging {
  
  type T <: Event
  
  val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 60 seconds) {
	 case _: EventCreationException => Stop
	 case _: Exception => Restart
  }

  override protected def process(event: T) {
    log.debug("about to process event: " + event)

    val eventId = getEventId(event)    
    log.debug("looking for active event builder with id: " + eventId)
    val runningActivity = getBuildActor(eventId)
    
    runningActivity ! event    
  }

  def getEventId(event: T): String
  
  def createBuildActor(id: String): BuildActor
  
  def getBuildActor(eventId: String): ActorRef = context.actorFor(eventId) match {
    case notRunning: ActorRef if(notRunning.isTerminated) => context.actorOf(Props(createBuildActor(eventId)), eventId)
    case actor: ActorRef => {
     log.debug("found: " + actor)
     actor
    }
  }
  
}