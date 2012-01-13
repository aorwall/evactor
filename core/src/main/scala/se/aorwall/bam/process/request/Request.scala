package se.aorwall.bam.process.request
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

class Request (processorId: String, timeout: Long) extends ProcessorConfiguration(processorId: String){

  override def getProcessor(): Processor = {
    new RequestProcessor(processorId, timeout)
  }
  
}