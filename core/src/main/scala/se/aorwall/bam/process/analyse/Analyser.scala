package se.aorwall.bam.process.analyse

import akka.actor.{Actor, ActorRef, ActorLogging}
import scala.Predef._
import se.aorwall.bam.model.events.{Event, AlertEvent}
import se.aorwall.bam.process._

/**
 * Analyses event flows and creates alert events.
 */
abstract class Analyser(
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val category: Option[String]) 
  extends Processor(subscriptions) 
  with Publisher
  with Monitored
  with ActorLogging  {

  var triggered = false 
  
  protected def alert(message: String) {
    if (!triggered) {
      log.warning("Alert: " + message)

      triggered = true
      sendAlert( message)
    }
  }

  protected def backToNormal() {
    if (triggered) {
      log.info("Back to normal")

      triggered = false
      sendAlert("Back to normal")
    }
  }

  def sendAlert(message: String) {
    val currentTime = System.currentTimeMillis
    val alert = 
      new AlertEvent(
        channel,
        category,
        currentTime.toString, 
        currentTime, 
        triggered, 
        message)
    
    log.info(alert.toString)
    publish(alert)

    // If a test actor exists
    testActor match {
      case Some(actor) => actor ! alert
      case _ =>
    }
  }

}