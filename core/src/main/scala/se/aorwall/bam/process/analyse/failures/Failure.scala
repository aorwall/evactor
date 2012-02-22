package se.aorwall.bam.process.analyse.failures

import se.aorwall.bam.process.analyse.window.LengthWindow
import se.aorwall.bam.process.analyse.window.LengthWindowConf
import se.aorwall.bam.process.analyse.window.TimeWindow
import se.aorwall.bam.process.analyse.window.TimeWindowConf
import se.aorwall.bam.process.analyse.window.WindowConf
import se.aorwall.bam.process.ProcessorConfiguration

class Failure (
    name: String, 
    eventName: Option[String], 
    maxOccurrences: Long, 
    window: Option[WindowConf])
  extends ProcessorConfiguration(name) {

  def getProcessor = window match {
    case Some(length: LengthWindowConf) => 
      new FailureAnalyser(name, eventName, maxOccurrences) 
        with LengthWindow { override val noOfRequests = length.noOfRequests }
    case Some(time: TimeWindowConf) => 
      new FailureAnalyser(name, eventName, maxOccurrences) 
        with TimeWindow {override val timeframe = time.timeframe}
    case None => 
      new FailureAnalyser(name, eventName, maxOccurrences)
  }
}