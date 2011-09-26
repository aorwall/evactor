package se.aorwall.logserver.process.dynamic

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.process.{ActivityBuilder, BusinessProcess}
import se.aorwall.logserver.model.{Log, Activity, State}
import se.aorwall.logserver.process.ActivityException

/**
 * Create a new process based on the componentId in the incoming log event
 */
class DynamicComponent (val timeout: Long) extends BusinessProcess with Logging {

  val processId = "dynamicComponent"

  /*
   * Accepts all componentId:s
   */
  def contains(componentId: String) = true

  def getActivityId(logevent: Log) = logevent.componentId + ":" + logevent.correlationId

  def getActivityBuilder(): ActivityBuilder = new DynamicComponentActivityBuilder()

  /**
   * Check if state = START
   */
  def startNewActivity(logevent: Log) =  logevent.state == State.START
}

class DynamicComponentActivityBuilder () extends ActivityBuilder {

  var startEvent: Option[Log] = None
  var endEvent: Option[Log] = None

  def addLogEvent(logevent: Log): Unit = {
     if(logevent.state == State.START){
       startEvent = Some(logevent)
     } else if(logevent.state >= 10) {
       endEvent = Some(logevent)
     }
  }

  def isFinished(): Boolean = startEvent != None && endEvent != None

  def createActivity() = (startEvent, endEvent) match {
    case (Some(start: Log), Some(end: Log)) =>
      new Activity(end.componentId, end.correlationId, end.state, start.timestamp, end.timestamp)
    case (Some(start: Log), _) =>
      new Activity(start.componentId, start.correlationId, State.TIMEOUT, start.timestamp, 0L)
    case (_, end: Log) =>
       throw new ActivityException("DynamicComponentActivityBuilder was trying to create a activity with only an end log event. End event: " + end)
    case (_, _) =>
       throw new ActivityException("DynamicComponentActivityBuilder was trying to create a activity without either a start or an end log event.")
  }

  def clear() = {
    startEvent = None
    endEvent = None
  }

}