package se.aorwall.bam.process.alert

import se.aorwall.bam.process.Processor
import akka.actor.ActorLogging
import se.aorwall.bam.process.Subscription

abstract class Alerter (
    override val subscriptions: List[Subscription]) 
  extends Processor (subscriptions)
  with ActorLogging {
  
}