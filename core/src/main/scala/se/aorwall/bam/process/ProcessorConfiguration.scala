package se.aorwall.bam.process
import akka.actor.Actor

abstract class ProcessorConfiguration (val processorId: String) {
  
  def getProcessor(): Actor
  
}