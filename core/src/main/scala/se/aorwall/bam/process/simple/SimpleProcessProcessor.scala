package se.aorwall.bam.process.simple

import grizzled.slf4j.Logging
import se.aorwall.bam.process.EventCreationException
import se.aorwall.bam.process.EventBuilder
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.process.ProcessorActor
import se.aorwall.bam.process.Processor
import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.process.Timed

/**
 * Processes simple processes with a defined list of components requested. The process will complete when the first and the 
 * last event has been called.
 */
class SimpleProcessProcessor(val processorId: String, val components: List[String], val timeout: Long) extends Processor with Logging {

  type T = RequestEvent
  
  val componentMap = components.toSet

  override def receive = {
    case event: RequestEvent if(handlesEvent(event)) => sendToRunningProcessor(event)
    case _ => // skip
  }
  
  def handlesEvent(event: RequestEvent) = {
     componentMap.contains(event.name)
  }

  def getEventId(logevent: RequestEvent) = logevent.id

  def createProcessorActor(id: String): ProcessorActor = {
    new ProcessorActor(id, new SimpleProcessEventBuilder(processorId, components) ) with Timed { _timeout = Some(timeout) }
  } 

  override def toString = "SimpleProcess ( id: " + processorId + ", components: " + components + ")"

}

class SimpleProcessEventBuilder(val processId: String, val components: List[String]) extends EventBuilder with Logging {
    
  var startEvent: Option[RequestEvent] = None
  var endEvent: Option[RequestEvent] = None
  var requests = List[RequestEvent]()
  
  val endComponent = components.last
  
  val endStates = Set(State.FAILURE, State.CANCELLATION, State.TIMEOUT)

  def addEvent(event: Event) = event match {
    case reqEvent: RequestEvent => addRequestEvent(reqEvent)  
    case _ =>    
  }

  def addRequestEvent(event: RequestEvent) {
	requests :+ event
	
    if(components.head == event.name)      
       startEvent = Some(event)
    else if(endComponent == event.name)
       endEvent = Some(event)    
    else if(endStates.contains(event.state) ){
       endEvent = Some(event)
    }
  }
    
  def isFinished(): Boolean = (startEvent, endEvent) match {
     case (Some(start: RequestEvent), Some(end: RequestEvent)) => true
     case (Some(start: RequestEvent), _) if (endStates.contains(start.state))=> true 
     case msg => false
  }

  def createEvent(): SimpleProcessEvent = (startEvent, endEvent) match {
    case (Some(start: RequestEvent), Some(end: RequestEvent)) =>
      new SimpleProcessEvent(processId, end.id, end.timestamp, requests, end.state, end.timestamp - start.timestamp + start.latency )
    case (Some(start: RequestEvent), _) => 
      new SimpleProcessEvent(processId, start.id, System.currentTimeMillis, requests, getState(start), 0L)
    case (_, end: RequestEvent) =>
       throw new EventCreationException("SimpleProcessEventBuilder was trying to create an event with only an end request event. End event: " + end)
    case (_, _) =>
       throw new EventCreationException("SimpleProcessEventBuilder was trying to create an event without either a start or an end event.")
  }

  protected def getState(reqEvent: RequestEvent): Int = 
  	if(endStates.contains(reqEvent.state)) reqEvent.state
  	else State.TIMEOUT
  
  def clear() {
    startEvent = None
    endEvent = None
    requests = List[RequestEvent]()
  }
}