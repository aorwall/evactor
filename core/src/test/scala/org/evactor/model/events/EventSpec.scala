/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.model.events

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.evactor.EvactorSpec

@RunWith(classOf[JUnitRunner])
class EventSpec extends EvactorSpec {

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