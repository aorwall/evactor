package se.aorwall.bam.process.analyse.latency

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.analyse.window.TimeWindow
import se.aorwall.bam.process.analyse.window.LengthWindow
import se.aorwall.bam.process.analyse.window.WindowConf
import se.aorwall.bam.process.analyse.window.TimeWindowConf
import se.aorwall.bam.process.analyse.window.LengthWindowConf

class Latency (
    name: String, 
    eventName: Option[String], 
    maxLatency: Long, 
    window: Option[WindowConf])
  extends ProcessorConfiguration(name) {

  def processor = window match {
    case Some(length: LengthWindowConf) => 
      new LatencyAnalyser(name, eventName, maxLatency) 
        with LengthWindow { override val noOfRequests = length.noOfRequests }
    case Some(time: TimeWindowConf) => 
      new LatencyAnalyser(name, eventName, maxLatency)
        with TimeWindow { override val timeframe = time.timeframe }
    case None => new LatencyAnalyser(name, eventName, maxLatency)
  }
}