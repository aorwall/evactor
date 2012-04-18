package se.aorwall.bam.process.analyse

import akka.actor.{Actor, ActorRef, ActorLogging}
import scala.Predef._
import se.aorwall.bam.model.events.{Event, AlertEvent}
import se.aorwall.bam.process._
import se.aorwall.bam.model.events.EventRef

/**
 * An analyser analyses event flows and creates alert events when 
 * triggered. It will only create an alert the first time it's 
 * triggered and then wait until state is back to normal again.
 * 
 * TODO: Should do some kind of check on timestamp on events if
 * events arrive in the wrong order.
 * 
 */
abstract class Analyser(
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val category: Option[String]) 
  extends Processor(subscriptions) 
  with Publisher
  with Monitored
  with ActorLogging {

  var triggered = false 
  
  protected def alert(message: String, event: Option[Event] = None) {
    if (!triggered) {
      log.debug("Alert: {}", message)
      triggered = true
      sendAlert( message, event)
    }
  }

  protected def backToNormal(event: Option[Event] = None) {
    if (triggered) {
      log.debug("Back to normal")

      triggered = false
      sendAlert("Back to normal", event)
    }
  }

  def sendAlert(message: String, event: Option[Event]) {
    val currentTime = System.currentTimeMillis
    
    val eventRef = event match {
      case Some(e) => Some(EventRef(e))
      case None => None
    }
    
    val alert = 
      new AlertEvent(
        channel,
        category,
        currentTime.toString, 
        currentTime, 
        triggered, 
        message,
        eventRef)
    
    publish(alert)

    // If a test actor exists
    testActor match {
      case Some(actor) => actor ! alert
      case _ =>
    }
  }
  
}