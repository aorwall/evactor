package se.aorwall.bam.model.events

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.BamSpec

@RunWith(classOf[JUnitRunner])
class EventSpec extends BamSpec {

  "An Event" should {
     
    "be able to clone itself to a new event with a different channel and category" in {      
      val event = new Event("channel", None, "id", 0L)  
      val newEvent = event.clone("foo", Some("bar"))
      
      newEvent.channel should be ("foo")
      newEvent.category should be (Some("bar"))
    }
    
  }
  
  "An EventRef" should {
     
    "be created by providing a correct event URI" in {      
      val eventRef = EventRef.fromString("Event://foobar")
      
      eventRef.className should be ("Event")      
      eventRef.id should be ("foobar")
    }
    
    "be created by providing an event" in {      
      val event = createEvent()
      val eventRef = EventRef(event)
    
      eventRef.className should be ("Event")  
      eventRef.id should be ("id")
    } 
    
    "return a formatted string" in {
      val uri = "Event://foobar"
      val eventRef = EventRef.fromString(uri)
      
      eventRef.toString should be (uri)
    }
  }    
}