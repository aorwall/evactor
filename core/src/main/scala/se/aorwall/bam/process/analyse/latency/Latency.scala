package se.aorwall.bam.process.analyse.latency

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.analyse.window.TimeWindow
import se.aorwall.bam.process.analyse.window.LengthWindow
import se.aorwall.bam.process.analyse.window.WindowConf
import se.aorwall.bam.process.analyse.window.TimeWindowConf
import se.aorwall.bam.process.analyse.window.LengthWindowConf
import se.aorwall.bam.process.Subscription
import se.aorwall.bam.utils.JavaHelpers.any2option

class Latency (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val channel: String, 
    val category: Option[String],
    val maxLatency: Long, 
    val window: Option[WindowConf])
  extends ProcessorConfiguration(name, subscriptions) {

  def this(name: String, subscription: Subscription, 
    channel: String, category: String, 
    maxLatency: Long, window: WindowConf) = {
    this(name, List(subscription), channel, category, maxLatency, window)
  }
  
  def processor = window match {
    case Some(length: LengthWindowConf) => 
      new LatencyAnalyser(subscriptions, channel, category, maxLatency) 
        with LengthWindow { override val noOfRequests = length.noOfRequests }
    case Some(time: TimeWindowConf) => 
      new LatencyAnalyser(subscriptions, channel, category, maxLatency)
        with TimeWindow { override val timeframe = time.timeframe }
    case None => new LatencyAnalyser(subscriptions, channel, category, maxLatency)
  }
}