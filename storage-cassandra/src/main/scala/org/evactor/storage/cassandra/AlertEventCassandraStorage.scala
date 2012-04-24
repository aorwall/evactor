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

import akka.actor.ActorContext
import akka.actor.ActorSystem
import me.prettyprint.hector.api.beans.ColumnSlice
import org.evactor.storage.EventStorage
import org.evactor.model.events.Event
import org.codehaus.jackson.map.ObjectMapper
import org.evactor.model.events.AlertEvent

class AlertEventCassandraStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {
	type EventType = AlertEvent

	def this(system: ActorSystem) = this(system, "AlertEvent")
			
  override val columnNames = List("id", "timestamp", "triggered", "message", "eventRef")
	   
  def eventToColumns(event: Event): List[(String, String)] = event match {  
	    case alertEvent: AlertEvent => 
	      ("id", event.id) :: 
	      ("timestamp", event.timestamp.toString) :: 
	      ("triggered", alertEvent.triggered.toString) :: 
	      ("message", alertEvent.message) :: 
	      getEventRefCol("eventRef", alertEvent.eventRef) :::	Nil
	    case _ => throw new RuntimeException("Type not supported: " + event.getClass().getName()) // TODO: Fix some kind of storage exception...
	}
	
  def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	  val get = getValue(columns) _	
	  new AlertEvent(
		        "",
		        None,
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("triggered").toBoolean,
			 			get("message"),
			 			getEventRef(columns, "eventRef"))
	}
}
