package se.aorwall.bam.process.simple

import collection.immutable.Map
import grizzled.slf4j.Logging
import se.aorwall.bam.model.process.{ActivityBuilder, BusinessProcess}
import se.aorwall.bam.model.{Log, Activity, State}
import se.aorwall.bam.process.ActivityException

class SimpleProcess(val processId: String, val components: List[Component], val timeout: Long) extends BusinessProcess with Logging {

  val componentMap = components.map{ comp: Component => comp.componentId }.toSet

  def handlesEvent(log: Log) = {
     componentMap.contains(log.componentId)
  }

  def getActivityId(logevent: Log) = logevent.correlationId

  def getActivityBuilder(): ActivityBuilder = {
    new SimpleActivityBuilder(processId, components, components map { comp: Component => (comp.componentId, comp.maxRetries) } toMap)
  }

  override def toString = "SimpleProcess ( id: " + processId + ", components: " + components + ")"

}

class SimpleActivityBuilder(val processId: String, val components: List[Component], var retries: Map[String, Int]) extends ActivityBuilder with Logging {

  var startEvent: Option[Log] = None
  var endEvent: Option[Log] = None

  val failureStates = Set(State.INTERNAL_FAILURE, State.CLIENT_FAILURE, State.UNKNOWN_FAILURE)
  val endComponent = components.last

  def addLogEvent(logevent: Log) {

    if(components.head.componentId == logevent.componentId && logevent.state == State.START)
       startEvent = Some(logevent)
    else if(endComponent.componentId == logevent.componentId && logevent.state == State.SUCCESS)
       endEvent = Some(logevent)
    else if(failureStates.contains(logevent.state) )
       endEvent = Some(logevent)
    else if (logevent.state == State.BACKEND_FAILURE) {
      val remainingRetries = retries.getOrElse(endComponent.componentId, 0)
      if(remainingRetries <= 0)
         endEvent = Some(logevent)
      else
         retries = retries + (endComponent.componentId -> (remainingRetries-1)) //TODO
    }
  }

  def isFinished(): Boolean = startEvent != None && endEvent != None

  def createActivity() = (startEvent, endEvent) match {
    case (Some(start: Log), Some(end: Log)) =>
      new Activity(processId, end.correlationId, end.state, start.timestamp, end.timestamp)
    case (Some(start: Log), _) =>
      new Activity(processId, start.correlationId, State.TIMEOUT, start.timestamp, 0L)
    case (_, end: Log) =>
       throw new ActivityException("SimpleActivityBuilder was trying to create a activity with only an end log event. End event: " + end)
    case (_, _) =>
       throw new ActivityException("SimpleActivityBuilder was trying to create a activity without either a start or an end log event.")
  }

  def clear() {
    startEvent = None
    endEvent = None
  }
}