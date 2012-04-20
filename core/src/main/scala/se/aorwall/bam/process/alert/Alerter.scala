package se.aorwall.bam.process.alert

import se.aorwall.bam.process.Processor
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

/**
 * Alerter sends alerts about events to external parties and
 * should not create new events.
 */
abstract class Alerter (
    override val subscriptions: List[Subscription]) 
  extends Processor (subscriptions)
  with ActorLogging {
  
}