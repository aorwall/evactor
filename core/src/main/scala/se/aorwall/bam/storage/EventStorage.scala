package se.aorwall.bam.storage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.events.LogEvent
import java.util.Date

trait Storage {

  // read conf from akka.conf?
    
  /**
   * Store an event and returns the persisted event. I the event couldn't be
   * saved nothing will be returned
   * 
   * TODO: It shouldn't be necessary to edit this files when new event types is added
   */
  def storeEvent(event: Event): Option[Event] = {
    event match {  
      case logEvent: LogEvent => Some(logEvent)
      case requestEvent: RequestEvent => Some(requestEvent)
      case simpleProcessEvent: SimpleProcessEvent => Some(simpleProcessEvent)
      case _ => None
    }
    
  }
  
  def readEvents(name: String, from: Date, to: Date, count: Int, start: Int): List[Event] = {
    List[Event]()
  } 
  

}