package se.aorwall.bam.process.build

import java.util.concurrent.{TimeUnit, ScheduledFuture}
import akka.actor._
import akka.util.duration._
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.Timeout
import se.aorwall.bam.process.ProcessorEventBus
import se.aorwall.bam.process.ProcessorEventBusExtension
import se.aorwall.bam.process.Publisher

/**
 * One build actor for each running event builder
 */
abstract class BuildActor(id: String, timeout: Long) extends EventBuilder with Publisher with ActorLogging {
  
  var testAnalyser: Option[ActorRef] = None // Used for test

  var scheduledFuture: Option[Cancellable] = None
  
  override def preStart() {
    log.debug("starting...")
    scheduledFuture = Some(context.system.scheduler.scheduleOnce(timeout milliseconds, self, Timeout))
  }
  
  override def postRestart(reason: Throwable) {
  
    // TODO: in case of a restart, read active events from db (CF: activeEvent/name/id/****
    //storedEvents.foreach(event => eventBuilder.addEvent(event))
    
    if(isFinished){
      sendEvent(createEvent.fold(throw _, e => e))
    }
  }

  def receive = {
    case event: Event => process(event)
    case Timeout => log.debug("%s: timeout!".format(id)); sendEvent(createEvent.fold(throw _, e => e))
    case actor: ActorRef => testAnalyser = Some(actor)
    case msg => log.info("can't handle: {}", msg)
  }

  def process(event: Event) {

    //TODO: Save event to in activeEvents

    if(log.isDebugEnabled) log.debug("received event : %s".format(event) )

    addEvent(event)

    if(isFinished){
      log.debug("finished!")
      sendEvent(createEvent.fold(throw _, e => e))
    }
  }

  def sendEvent(event: Event) {

    // send the created event back to the processor event bus
    publish(event)

    // If a test actor exists
    testAnalyser match {
      case Some(testActor) => testActor ! event
      case _ =>
    }
    
    stopScheduler()
    context.stop(context.self)
  }

  def eventExists(activityId: String) = false
  // TODO: Check if a finished event already exists
    
  override def postStop() {
    log.debug("stopping...")
    stopScheduler()
    clear()
  }
  
  private[this] def stopScheduler() = scheduledFuture match {
    case Some(s) => s.cancel()
    case None => 
  }

}

