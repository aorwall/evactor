package se.aorwall.bam.process.build.request

import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Failure
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.model.Timeout
import se.aorwall.bam.process.build.EventBuilder
import se.aorwall.bam.process.build.EventCreationException
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.build.BuildActor
import se.aorwall.bam.process.build.Timed
import se.aorwall.bam.process.build.Builder
import akka.actor.ActorRef
import se.aorwall.bam.process.ProcessorEventBus
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscriber

/**
 * Handles LogEvent objects and creates a RequestEvent object. 
 * 
 * TODO: Implement some rules for which LogEvents a RequestProcessor should handle. Like regex and stuff...
 */
class RequestBuilder (override val name: String, val timeout: Long) 
  extends Builder(name) 
  with Subscriber 
  with ActorLogging {
  
  type T = LogEvent
     
  override def preStart = {
    log.debug("subscribing to get all log events")
    subscribe(context.self, classOf[LogEvent].getSimpleName + "/*")
  }
  
  override def postStop = {
    log.debug("unsubscribing")
    unsubscribe(context.self, classOf[LogEvent].getSimpleName + "/*")
  }
   
  override def receive = {
	    case event: LogEvent => process(event) // TODO: case event: T  doesn't work...
	    case actor: ActorRef => testActor = Some(actor) 
	    case _ => // skip
  }
  
  /*
   * Accepts all componentId:s
   */
  def handlesEvent(event: LogEvent) = true

  def getEventId(logevent: LogEvent) = logevent.name + "_" + logevent.correlationId

  def createBuildActor(id: String): BuildActor = {
    new BuildActor(id, timeout) with RequestEventBuilder
  } 
  
}

trait RequestEventBuilder extends EventBuilder with ActorLogging {
  
  var startEvent: Option[LogEvent] = None
  var endEvent: Option[LogEvent] = None

  def addEvent(event: Event) = event match {
    case logevent: LogEvent => addLogEvent(logevent)  
    case _ =>    
  }

  def addLogEvent(logevent: LogEvent) {
	 logevent.state match {
	   case Start => startEvent = Some(logevent)
	   case Success => endEvent = Some(logevent)
	   case Failure => endEvent = Some(logevent)
	   case state => log.warning("Unknown state on log event: " + state)
	 }
  }

  def isFinished(): Boolean = startEvent != None && endEvent != None

  def createEvent(): Either[Throwable, RequestEvent] = (startEvent, endEvent) match {
    case (Some(start: LogEvent), Some(end: LogEvent)) =>
      Right(new RequestEvent(end.name, end.correlationId, end.timestamp, Some(EventRef(start)), Some(EventRef(end)), end.state, end.timestamp - start.timestamp ))
    case (Some(start: LogEvent), _) =>
      Right(new RequestEvent(start.name, start.correlationId, System.currentTimeMillis, Some(EventRef(start)), None, Timeout, 0L))
    case (_, end: LogEvent) =>
      Left(new EventCreationException("RequestProcessor was trying to create an event with only an end log event. End event: " + end))
    case (_, _) =>
      Left(new EventCreationException("RequestProcessor was trying to create an event without either a start or an end log event."))
  }

  def clear() {
    startEvent = None
    endEvent = None
  }

}