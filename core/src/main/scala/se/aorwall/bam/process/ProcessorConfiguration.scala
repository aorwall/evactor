package se.aorwall.bam.process

abstract class ProcessorConfiguration (val processorId: String) {
  
  def getProcessor(): Processor
  
}