package se.aorwall.bam.process.build.request

import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor
import se.aorwall.bam.process.Subscription

class Request (
    override val name: String,
    override val subscriptions: List[Subscription], 
    val timeout: Long) 
  extends ProcessorConfiguration(name, subscriptions) {

  override def processor: Processor = {
    new RequestBuilder(subscriptions, timeout: Long)
  }
  
}
