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
package org.evactor.storage

import scala.reflect.BeanInfo
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import org.evactor.model.events.Event
import org.evactor.model.events.LogEvent
import org.evactor.model.Start
import org.evactor.EvactorSpec
import org.evactor.model.Message

class TestEventStorage(override val system: ActorSystem) extends EventStorage(system) {
 
  def storeMessage(message: Message): Unit = {

  }
  
  def getEvent(id: String): Option[Event] = None
  
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    List[Event]()
  }
    
  def getStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    (0L, List[Long]())
  }
  
  def getEventCategories(channel: String, count: Int): List[(String, Long)] = {
    List[(String, Long)]()
  }
  
  def eventExists(event: Event) = false
  
  
  def getEventChannels(count: Int): List[(String, Long)] = {
    List[(String, Long)]() 
  }
  
}

object EventStorageSpec {

  val storageConf = ConfigFactory.parseString("""
		akka {
		  evactor {
		    storage {
		        
		      implementation = org.evactor.storage.TestEventStorage
		    
		    }
		  }
		}
		""")
	
}

@RunWith(classOf[JUnitRunner])
class EventStorageSpec(system: ActorSystem) extends EvactorSpec {

  def this() = this( ActorSystem("EventStorageSpec", EventStorageSpec.storageConf) )
  
  val store = EventStorageExtension(system)
    
  val logEvent = createLogEvent(0L, Start)
  
  "EventStorage" must {

    "returns the right event storage implementation" in {     
      store.getEventStorage match {
        case Some(e) => e.getClass.getName must be ("org.evactor.storage.TestEventStorage")
        case e => fail("expected an instance of org.evactor.storage.TestEventStorage but found: " + e)
      }    
    }
  }
    
}