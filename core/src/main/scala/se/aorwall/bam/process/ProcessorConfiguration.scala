package se.aorwall.bam.process
import akka.actor.Actor

/**
 * Base class for process configurations. 
 * 
 * name is the name that will be set to the new event
 */
abstract class ProcessorConfiguration (val name: String) {
  
  def processor: Processor
  
}