package se.aorwall.bam.storage

import scala.reflect.BeanInfo
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.BamSpec

class TestEventStorage(system: ActorSystem) extends EventStorage {
 
  def storeEvent(event: Event): Unit = {

  }
  
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    List[Event]()
  }
    
  def getStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    (0L, List[Long]())
  }
  
  def getEventCategories(channel: String, count: Int): Map[String, Long] = {
    Map[String, Long]()
  }
  
  def eventExists(event: Event) = false
  
}

object EventStorageSpec {

  val storageConf = ConfigFactory.parseString("""
		akka {
		  bam {
		    storage {
		        
		      implementations {
		        event = se.aorwall.bam.storage.TestEventStorage
		      }
		
		      storage-bindings {
		        event = ["se.aorwall.bam.model.events.LogEvent"] 
		      }
		    
		    }
		  }
		}
		""")
	
}

@RunWith(classOf[JUnitRunner])
class EventStorageSpec(system: ActorSystem) extends BamSpec with Logging {

  def this() = this( ActorSystem("EventStorageSpec", EventStorageSpec.storageConf) )
  
  val store = EventStorageExtension(system)
    
  val logEvent = createLogEvent(0L, Start)
  
  "EventStorage" must {

    "have correct bindings" in {
      store.bindings(logEvent.getClass.getName) must be("event")
    }

    "returns the right event storage implementation" in {     
      store.getEventStorage(logEvent) match {
        case Some(e) => e.getClass.getName must be ("se.aorwall.bam.storage.TestEventStorage")
        case e => fail("expected an instance of se.aorwall.bam.storage.TestEventStorage but found: " + e)
      }    
    }
  }
    
}