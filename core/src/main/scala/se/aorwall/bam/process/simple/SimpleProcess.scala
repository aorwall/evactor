package se.aorwall.bam.process.simple

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

class SimpleProcess (processorId: String, components: List[String], timeout: Long) extends ProcessorConfiguration(processorId: String){

  def getProcessor(): Processor = {
    new SimpleProcessProcessor(processorId, components, timeout)
  }
  
}