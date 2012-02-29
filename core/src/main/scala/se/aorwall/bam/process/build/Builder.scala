package se.aorwall.bam.process.build

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.{Processor, Monitored}
import scala.collection.mutable.HashMap

abstract class Builder (override val name: String) 
  extends Processor (name) 
  with Monitored
  with ActorLogging {
  
  type T <: Event
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 60 seconds) {
	 case e: EventCreationException => log.warning(name + ": stopping on exception!"); Stop
	 case e: Exception => Restart
  }

  override protected def process(event: T) {
    if(log.isDebugEnabled) log.debug("about to process event: " + event)

    val eventId = getEventId(event)    
    if(log.isDebugEnabled) log.debug("looking for active event builder with id: " + eventId)
    val actor = getBuildActor(eventId)
    
    actor ! event
  }

  def getEventId(event: T): String
  
  def createBuildActor(id: String): BuildActor
  
  def getBuildActor(eventId: String): ActorRef = context.children.find(_.path.name == eventId).getOrElse(context.actorOf(Props(createBuildActor(eventId)), eventId))
  
}