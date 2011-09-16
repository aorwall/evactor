package se.aorwall.logserver.process.dynamic

import grizzled.slf4j.Logging
import se.aorwall.logserver.model.process.{ActivityBuilder, BusinessProcess}
import se.aorwall.logserver.model.{Activity, State, LogEvent}

/**
 * Create a new process based on the componentId in the incoming log event
 */
class DynamicComponent () extends BusinessProcess with Logging {

  val processId = "dynamicComponent"

  /*
   * Accepts all componentId:s
   */
  def contains(componentId: String) = true

  def getActivityId(logevent: LogEvent) = logevent.componentId + ":" + logevent.correlationId

  def getActivityBuilder(): ActivityBuilder = new DynamicComponentActivityBuilder()

  /**
   * Check if state = START
   */
  def startNewActivity(logevent: LogEvent) =  logevent.state == State.START
}

class DynamicComponentActivityBuilder () extends ActivityBuilder {

  var startEvent: LogEvent = null  //TODO: Fix null?
  var endEvent: LogEvent = null

  def addLogEvent(logevent: LogEvent): Unit = {
     if(logevent.state == State.START){
       startEvent = logevent
     } else if(logevent.state >= 10) {
       endEvent = logevent
     }
  }

  def isFinished(): Boolean = startEvent != null && endEvent != null

  def createActivity() = {
    if(startEvent != null && endEvent != null){
      new Activity(endEvent.componentId, endEvent.correlationId, endEvent.state, startEvent.timestamp, endEvent.timestamp)
    } else if(startEvent != null) {
      new Activity(startEvent.componentId, startEvent.correlationId, State.TIMEOUT, startEvent.timestamp, 0L)
    } else if(endEvent != null) {
       throw new RuntimeException("DynamicComponentActivityBuilder was trying to create a activity with only an end log event. End event: " + endEvent)
    } else  {
       throw new RuntimeException("DynamicComponentActivityBuilder was trying to create a activity without either a start or an end log event.")
    }
  }
}