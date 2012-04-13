package se.aorwall.bam.process.build.simpleprocess

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.Subscription

class SimpleProcess (
    override val name: String,
    override val subscriptions: List[Subscription],
    val channel: String,
    val category: Option[String],
    val timeout: Long) 
  extends ProcessorConfiguration(name, subscriptions) {

  def processor: Processor = {
    new SimpleProcessBuilder(subscriptions, channel, category, timeout)
  }
  
}