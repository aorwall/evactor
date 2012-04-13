package se.aorwall.bam
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import BamSpec.category
import BamSpec.channel
import BamSpec.id
import BamSpec.timestamp
import se.aorwall.bam.model.events._
import se.aorwall.bam.model.State

object BamSpec {
  val channel = "channel"
  val category = Some("category")
  val id = "id"
  val timestamp = 0L 
}

trait BamSpec extends WordSpec with MustMatchers with ShouldMatchers {
  import BamSpec._
  
  def createDataEvent(message: String) = new DataEvent(channel, category, id, timestamp, message)
   
  def createEvent() = new Event(channel, category, id, timestamp)
  def createLogEvent(timestamp: Long, state: State) = new LogEvent(channel, category, id, timestamp, "329380921309", "client", "server", state, "hello")

  def createRequestEvent(timestamp: Long, inboundRef: Option[EventRef], outboundRef: Option[EventRef], state: State, latency: Long) = 
    new RequestEvent(channel, category, id, timestamp, inboundRef, outboundRef, state, latency)

}