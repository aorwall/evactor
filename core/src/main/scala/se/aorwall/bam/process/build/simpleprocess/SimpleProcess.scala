package se.aorwall.bam.process.build.simpleprocess

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

class SimpleProcess (name: String, val components: List[String], val timeout: Long) 
  extends ProcessorConfiguration(name: String) {

  def processor: Processor = {
    new SimpleProcessBuilder(name, components, timeout)
  }
  
}