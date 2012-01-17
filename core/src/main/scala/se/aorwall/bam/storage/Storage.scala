package se.aorwall.bam.storage
import java.util.Date

import akka.actor.Actor
import akka.dispatch.MailboxType
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.SimpleProcessEvent

trait Storage extends Actor {

	val storage = EventStorageExtension(context.system)

	/**
	 * Store an event and returns true if successful.
	 */
	def storeEvent(event: Event): Boolean = {	  
		storage.getEventStorage(event) match {
		  case Some(storageImpl) => storageImpl.storeEvent(event)
		  case None => true // TODO: Do nothing, return true...
		}
	}

	def readEvents(name: String, from: Date, to: Date, count: Int, start: Int): List[Event] = {
		List[Event]()
	} 
}
