package se.aorwall.logserver.model.process.simple

import collection.immutable.Map
import grizzled.slf4j.Logging
import se.aorwall.logserver.model.{Activity, State, LogEvent}
import se.aorwall.logserver.model.process.{ActivityBuilder, BusinessProcess}
import collection.mutable.ListBuffer

class SimpleProcess(val processId: String, val components: List[Component]) extends BusinessProcess with Logging {

  val componentMap = components map { comp: Component => comp.componentId } toSet

  def contains(componentId: String) = {
     componentMap.contains(componentId)
  }

  def getActivityBuilder(): ActivityBuilder = {

    new SimpleActivityBuilder(processId, components, components map { comp: Component => (comp.componentId, comp.maxRetries) } toMap)
  }

  /**
   * Check if first component is the same as the one in the logevent and state is START
   */
  def startNewActivity(logevent: LogEvent) =
    components.head.componentId == logevent.componentId && logevent.state == State.START

  override def toString() = "SimpleProcess ( id: " + processId + ", components: " + components + ")"

}

class SimpleActivityBuilder(val processId: String, val components: List[Component], var retries: Map[String, Int]) extends ActivityBuilder with Logging {

  var startEvent: LogEvent = null  //TODO: Fix null?
  var endEvent: LogEvent = null

  val failureStates = Set(State.INTERNAL_FAILURE, State.CLIENT_FAILURE, State.UNKNOWN_FAILURE)
  val endComponent = components.last

  def addLogEvent(logevent: LogEvent): Unit = {

    if(components.head.componentId == logevent.componentId && logevent.state == State.START)
       startEvent = logevent
    else if(endComponent.componentId == logevent.componentId && logevent.state == State.SUCCESS)
       endEvent = logevent
    else if(failureStates.contains(logevent.state) )
       endEvent = logevent
    else if (logevent.state == State.BACKEND_FAILURE) {
      val remainingRetries = retries.getOrElse(endComponent.componentId, 0)
      if(remainingRetries <= 0)
         endEvent = logevent
      else
         retries =  retries + (endComponent.componentId -> (remainingRetries-1)) //TODO
    }
  }

  def isFinished(): Boolean = startEvent != null && endEvent != null

  def createActivity() = {
    if(startEvent != null && endEvent != null){
      new Activity(processId, endEvent.correlationId, endEvent.state, startEvent.timestamp, endEvent.timestamp)
    } else if(startEvent != null) {
      new Activity(processId, startEvent.correlationId, State.TIMEOUT, startEvent.timestamp, 0L)
    } else if(endEvent != null) {
       throw new RuntimeException("SimpleActivityBuilder was trying to create a activity with only an end log event. End event: " + endEvent)
    } else  {
       throw new RuntimeException("SimpleActivityBuilder was trying to create a activity without either a start or an end log event.")
    }
  }
}