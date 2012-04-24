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
package org.evactor.storage.cassandra

import org.evactor.model.events.LogEvent
import me.prettyprint.hector.api.Keyspace
import org.evactor.model.events.SimpleProcessEvent
import akka.actor.ActorContext
import akka.actor.ActorSystem
import org.evactor.storage.EventStorage
import me.prettyprint.hector.api.beans.ColumnSlice
import org.evactor.model.events.RequestEvent
import org.evactor.model.Start
import org.evactor.model.events.Event
import org.evactor.model.State
import org.evactor.model.events.EventRef

class SimpleProcessEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = SimpleProcessEvent
	
   def this(system: ActorSystem) = this(system, "SimpleProcessEvent")
   
	override val columnNames = List("id", "timestamp", "requests", "state", "latency")

   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new SimpleProcessEvent("", None,
	 	    get("id"),
	 	    get("timestamp").toLong,
	 	    getEventRefs(columns, "requests"),
	 		  State(get("state")),
	 		  java.lang.Long.parseLong(get("latency")))
	}
	
  	def eventToColumns(event: Event): List[(String, String)] = {		  
		event match {  
			case requestEvent: SimpleProcessEvent => 
				("id", event.id) ::
				("timestamp", event.timestamp.toString) ::
				("requests", requestEvent.requests.map(_.toString()).mkString(",")) ::
				("state", requestEvent.state.toString) ::
				("latency", String.valueOf(requestEvent.latency)) :: Nil
		}
	}  
  	
  def getEventRefs(columns: ColumnSlice[String, String], name: String): List[EventRef] = {
    if(columns.getColumnByName(name) != null) columns.getColumnByName(name).getValue().split(",").map(EventRef.fromString(_)).toList
    else List[EventRef]()
  }
  	
}
