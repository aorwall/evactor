package se.aorwall.bam.process.build.simpleprocess

import se.aorwall.bam.model.events._
import se.aorwall.bam.model._
import se.aorwall.bam.process._
import se.aorwall.bam.process.build._
import akka.actor.ActorRef
import se.aorwall.bam.process.ProcessorEventBus
import akka.actor.ActorLogging
import grizzled.slf4j.Logging
import scala.collection._

/**
 * Processes simple processes with requests from subscribed channels. If no request has failed
 * a request from each channel, and category if specified, must be processed before a new simple 
 * process event is created.
 * 
 */
class SimpleProcessBuilder(
    override val subscriptions: List[Subscription],
    val _channel: String,
    val _category: Option[String],
    val _timeout: Long) 
  extends Builder(subscriptions) 
  with Subscriber 
  with ActorLogging {

  type T = RequestEvent
  
  val subscribedChannels: List[String] = subscriptions.map { sub =>
     sub.channel match {
       case Some(channel) => channel
       case None => throw new IllegalArgumentException("A channel must be set in all subscriptions")
     }
  }
   
  override def receive = {
    case event: RequestEvent => process(event) 
    case actor: ActorRef => testActor = Some(actor) 
    case _ => // skip
  }

  def getEventId(logevent: RequestEvent) = logevent.id

  def createBuildActor(id: String): BuildActor = 
    new BuildActor(id, _timeout) 
      with SimpleProcessEventBuilder {
   	  val channel = _channel
   	  val category = _category
   	  val steps = subscribedChannels.size
   	}

  override def toString = "SimpleProcess ( creates event on channel: %s, subscribing to channels: %s)".format(_channel, subscribedChannels)

}

trait SimpleProcessEventBuilder extends EventBuilder with ActorLogging {
    
  val steps: Int
  val channel: String 
  val category: Option[String]
  
  var requests: List[RequestEvent] = List()
  var processedChannels: Set[String] = Set()
  
  var failed = false
  
  def addEvent(event: Event) = event match {
    case reqEvent: RequestEvent => addRequestEvent(reqEvent)  
    case _ =>    
  }

  def addRequestEvent(event: RequestEvent) {
    if(!processedChannels.contains(event.channel)){
      requests ::= event
      processedChannels += event.channel
    }
  }
    
  def isFinished(): Boolean = {
    if(requests.size == steps) true
    else if ( requests.exists(isFailure(_)) ) true
    else false    
  }
  
  private[this] def isFailure(event: RequestEvent): Boolean = {
     event.state match {
         case Timeout => true
         case Cancellation => true
         case Failure => true
         case _ => false
       }  
  }
  
  private[this] def getCauseOfFailure(event: RequestEvent): State = {
    event.state match  {
         case Cancellation => Cancellation
         case Failure => Failure
         case _ => Timeout
    } 
  }
  

  def createEvent(): Either[Throwable, SimpleProcessEvent] = {
    
    val sortedRequests = requests.sortWith((e1, e2) => e1.timestamp < e2.timestamp)

    if (requests.size == steps){
      Right(new SimpleProcessEvent(channel, category, sortedRequests.last.id, sortedRequests.last.timestamp, sortedRequests.map(EventRef(_)), sortedRequests.last.state, sortedRequests.last.timestamp - sortedRequests.first.timestamp + sortedRequests.first.latency ))
    } else if (requests.size > 0){
      Right(new SimpleProcessEvent(channel, category, sortedRequests.last.id, sortedRequests.last.timestamp, sortedRequests.map(EventRef(_)), getCauseOfFailure(sortedRequests.last), sortedRequests.last.timestamp - sortedRequests.first.timestamp + sortedRequests.first.latency ))
    } else {
      Left(new EventCreationException("SimpleProcessEventBuilder was trying to create an event with no request events"))
    }
  }

  protected def getState(reqEvent: RequestEvent) = reqEvent.state match {
     case Cancellation => Cancellation
     case Failure => Failure
     case _ => Timeout
  }
  
  def clear() {    
    requests = List[RequestEvent]()
    processedChannels = Set()
  }
}