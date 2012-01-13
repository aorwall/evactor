package se.aorwall.bam.process.request

import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.State
import se.aorwall.bam.process.EventBuilder
import se.aorwall.bam.process.EventCreationException
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.ProcessorActor
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.Event

/**
 * Handles LogEvent objects and creates a RequestEvent object. 
 * 
 * TODO: Implement some rules for which LogEvents a RequestProcessor should handle. Like regex and stuff...
 */
class RequestProcessor (val processorId: String, val timeout: Long) extends Processor with Logging {
  
  type T = LogEvent
  
  override def receive = {
    case event: LogEvent => sendToRunningProcessor(event)
    case _ => // skip
  }
  
  /*
   * Accepts all componentId:s
   */
  def handlesEvent(event: LogEvent) = true

  def getEventId(logevent: LogEvent) = logevent.name + "_" + logevent.correlationId

  def createProcessorActor(id: String): ProcessorActor = {
    new ProcessorActor(id, new RequestEventBuilder() )
  } 
  
}

class RequestEventBuilder extends EventBuilder {

  var startEvent: Option[LogEvent] = None
  var endEvent: Option[LogEvent] = None

  def addEvent(event: Event) = event match {
    case logevent: LogEvent => addLogEvent(logevent)  
    case _ =>    
  }

  def addLogEvent(logevent: LogEvent) {
     if(logevent.state == State.START){
       startEvent = Some(logevent)
     } else if(logevent.state >= 10) {
       endEvent = Some(logevent)
     }
  }

  def isFinished(): Boolean = startEvent != None && endEvent != None

  def createEvent(): RequestEvent = (startEvent, endEvent) match {
    case (Some(start: LogEvent), Some(end: LogEvent)) =>
      new RequestEvent(end.name, end.correlationId, end.timestamp, Some(start), Some(end), end.state, end.timestamp - start.timestamp )
    case (Some(start: LogEvent), _) =>
      new RequestEvent(start.name, start.correlationId, System.currentTimeMillis, Some(start), None, State.TIMEOUT, 0L)
    case (_, end: LogEvent) =>
       throw new EventCreationException("RequestProcessor was trying to create an event with only an end log event. End event: " + end)
    case (_, _) =>
       throw new EventCreationException("RequestProcessor was trying to create an event without either a start or an end log event.")
  }

  def clear() {
    startEvent = None
    endEvent = None
  }

}