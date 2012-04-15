package se.aorwall.bam.storage
import java.util.Date
import akka.actor.Actor
import akka.dispatch.MailboxType
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.SimpleProcessEvent
import akka.actor.ActorLogging
import com.twitter.ostrich.stats.Stats

trait Storage extends Actor with ActorLogging {

  val storage = EventStorageExtension(context.system)

  /**
   * Store an event and returns true if successful.
   */
  def storeEvent(event: Event): Unit = storage.getEventStorage(event) match {
    case Some(storageImpl) => Stats.time("store_%s".format(event.getClass.getName)) { storageImpl.storeEvent(event) }
    case None => log.debug("No storage implementation found for event: {}", event) 
  }
  
  def readEvents(channel: String, category: Option[String], from: Date, to: Date, count: Int, start: Int): List[Event] = List[Event]() //TODO
   	
  def eventExists(event: Event): Boolean = storage.getEventStorage(event) match {
    case Some(storageImpl) => Stats.time("check_%s".format(event.getClass.getName)) { storageImpl.eventExists(event) }
    case None => {
      log.debug("No storage implementation found for event: {}", event) 
      false // Return false if no storage implementation is found
    } 
  }
}
