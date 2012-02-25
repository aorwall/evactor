package se.aorwall.bam.process.build.simpleprocess

import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.Cancellation
import se.aorwall.bam.model.Failure
import se.aorwall.bam.model.State
import se.aorwall.bam.model.State
import se.aorwall.bam.model.Timeout
import se.aorwall.bam.process.build.BuildActor
import se.aorwall.bam.process.build.Builder
import se.aorwall.bam.process.build.EventBuilder
import se.aorwall.bam.process.build.EventCreationException
import se.aorwall.bam.process.build.Timed
import akka.actor.ActorRef
import se.aorwall.bam.process.ProcessorEventBus
import akka.actor.ActorLogging
import grizzled.slf4j.Logging
import se.aorwall.bam.process.Subscriber
import se.aorwall.bam.process.Monitored

/**
 * Processes simple processes with a defined list of components requested. The process will complete when the first and the 
 * last event has been called.
 */
class SimpleProcessBuilder(
    val _name: String, 
    val _components: List[String], 
    val _timeout: Long) 
  extends Builder(_name) 
  with Subscriber 
  with ActorLogging {

  type T = RequestEvent
  
  val componentMap = _components.toSet
  
  override def preStart = {
    log.debug("subscribing to get request events with names: " + _components)
    _components.foreach(comp => subscribe(context.self, classOf[RequestEvent].getSimpleName + "/" + comp))
  }
  
  override def postStop = {
    log.debug("unsubscribing")
    _components.foreach(comp => unsubscribe(context.self, classOf[RequestEvent].getSimpleName + "/" + comp))
  }
   
  override def receive = {
	    case event: RequestEvent => if(handlesEvent(event)) process(event) 
	    case actor: ActorRef => testActor = Some(actor) 
	    case _ => // skip
	}
  
  override def handlesEvent(event: RequestEvent) = {
     componentMap.contains(event.name)
  }

  def getEventId(logevent: RequestEvent) = logevent.id

  def createBuildActor(id: String): BuildActor = 
    new BuildActor(id, _timeout) 
      with SimpleProcessEventBuilder {
   	  def name = _name
   	  def components = _components
   	}

  override def toString = "SimpleProcess ( id: " + name + ", components: " + _components + ")"

}

trait SimpleProcessEventBuilder extends EventBuilder with ActorLogging {
    
  def name: String 
  def components: List[String]
  
  var startEvent: Option[RequestEvent] = None
  var endEvent: Option[RequestEvent] = None
  var requests = List[RequestEvent]()
  
  val endComponent = components.last
  
  def addEvent(event: Event) = event match {
    case reqEvent: RequestEvent => addRequestEvent(reqEvent)  
    case _ =>    
  }

  def addRequestEvent(event: RequestEvent) {
	 requests :+ event
	
    if(components.head == event.name) {      
       startEvent = Some(event)
    }
	 
    if(endComponent == event.name) {
       endEvent = Some(event)    
    } else {
       event.state match {
         case Timeout => endEvent = Some(event)
         case Cancellation => endEvent = Some(event)
         case Failure => endEvent = Some(event)
         case _ =>
       }       
    }
  }
    
  def isFinished(): Boolean = (startEvent, endEvent) match {
     case (Some(start: RequestEvent), Some(end: RequestEvent)) => true
     case (Some(start: RequestEvent), _) => {
       start.state match {
         case Timeout => true
         case Cancellation => true
         case Failure => true
         case _ => false
       }
     } 
     case msg => false
  }

  def createEvent(): Either[Throwable, SimpleProcessEvent] = (startEvent, endEvent) match {
    case (Some(start: RequestEvent), Some(end: RequestEvent)) =>
      Right(new SimpleProcessEvent(name, end.id, end.timestamp, requests.map(EventRef(_)), end.state, end.timestamp - start.timestamp + start.latency ))
    case (Some(start: RequestEvent), _) => 
      Right(new SimpleProcessEvent(name, start.id, System.currentTimeMillis, requests.map(EventRef(_)), getState(start), 0L))
    case (_, end: RequestEvent) =>
      Left(new EventCreationException("SimpleProcessEventBuilder was trying to create an event with only an end request event. End event: " + end))
    case (_, _) =>
      Left(new EventCreationException("SimpleProcessEventBuilder was trying to create an event without either a start or an end event."))
  }

  protected def getState(reqEvent: RequestEvent) = reqEvent.state match {
     case Cancellation => Cancellation
     case Failure => Failure
     case _ => Timeout
  }
  
  def clear() {
    startEvent = None
    endEvent = None
    requests = List[RequestEvent]()
  }
}