package se.aorwall.bam.process.analyse.absence

import akka.actor.Actor._
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Subscription

class AbsenceOfRequests (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val category: Option[String],
    val timeFrame: Long)
  extends ProcessorConfiguration(name, subscriptions) {

  def processor = new AbsenceOfRequestsAnalyser(subscriptions, channel, category, timeFrame)

}