package se.aorwall.bam.process.analyse.absence

import akka.actor.Actor._
import se.aorwall.bam.process.ProcessorConfiguration

class AbsenceOfRequests (name: String, eventName: Option[String], timeFrame: Long)
  extends ProcessorConfiguration(name) {

  def processor = new AbsenceOfRequestsAnalyser(name, eventName, timeFrame)

}