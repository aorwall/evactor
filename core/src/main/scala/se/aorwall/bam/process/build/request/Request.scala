package se.aorwall.bam.process.build.request

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

class Request (name: String, timeout: Long) 
  extends ProcessorConfiguration(name: String) {

  override def processor: Processor = {
    new RequestBuilder(name: String, timeout: Long)
  }
  
}
