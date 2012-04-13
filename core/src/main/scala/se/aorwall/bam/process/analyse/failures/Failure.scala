package se.aorwall.bam.process.analyse.failures

import se.aorwall.bam.process.analyse.window.LengthWindow
import se.aorwall.bam.process.analyse.window.LengthWindowConf
import se.aorwall.bam.process.analyse.window.TimeWindow
import se.aorwall.bam.process.analyse.window.TimeWindowConf
import se.aorwall.bam.process.analyse.window.WindowConf
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Subscription

class Failure (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val category: Option[String],
    val maxOccurrences: Long, 
    val window: Option[WindowConf])
  extends ProcessorConfiguration(name, subscriptions) {

  def processor = window match {
    case Some(length: LengthWindowConf) => 
      new FailureAnalyser(subscriptions, channel, category, maxOccurrences) 
        with LengthWindow { override val noOfRequests = length.noOfRequests }
    case Some(time: TimeWindowConf) => 
      new FailureAnalyser(subscriptions, channel, category, maxOccurrences) 
        with TimeWindow { override val timeframe = time.timeframe }
    case None => 
      new FailureAnalyser(subscriptions, channel, category, maxOccurrences)
  }
}