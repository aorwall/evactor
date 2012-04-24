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
import me.prettyprint.cassandra.serializers.ObjectSerializer
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.factory.HFactory
import org.evactor.model.events.Event
import org.evactor.model.events.LogEvent
import org.evactor.storage.EventStorage
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import akka.actor.ActorSystem
import me.prettyprint.cassandra.serializers.LongSerializer
import org.evactor.model.State

class LogEventCassandraStorage(val system: ActorSystem) extends EventStorage {

  private val settings = CassandraStorageExtension(system)  
  val cluster = HFactory.getOrCreateCluster(settings.Clustername, new CassandraHostConfigurator(settings.Hostname + ":" + settings.Port))
  private val keyspace = HFactory.createKeyspace(settings.Keyspace, cluster)
    
  val LOG_EVENT_CF = "LogEvent";
  val NAMES_CF = "EventNames"
    
  def getEvent(id: String): Option[Event] = {
     val column = HFactory.createColumnQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
     		.setColumnFamily(LOG_EVENT_CF)
     		.setName("event")
     		.setKey(id)
     		.execute().get
     		
     column.getValue match {
       case e: LogEvent => Some(e)
       case _ => None
     }

  }
  
  def eventExists(event: Event): Boolean = {	 
	 val existingEvent = HFactory.createStringColumnQuery(keyspace)
            .setColumnFamily(LOG_EVENT_CF)
            .setKey(event.id)
            .setName("event")
            .execute()
            .get()
	        
    existingEvent != null
  }
  
  def storeEvent(event: Event): Unit = {
    
     val mutator = HFactory.createMutator(keyspace, StringSerializer.get);
     // TODO: check if event already exists
          
     // column family: LogEvent
     // row key: event.name
     // column key: event id
     // column value: event object
     mutator.insert(event.id, LOG_EVENT_CF, HFactory.createColumn("event", event, StringSerializer.get, ObjectSerializer.get))
  }
  
  def getEvents(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], count: Int, start: Int): List[Event] = {
    List[LogEvent]() // TODO
  }
  
  def getEventCategories(channel: String, count: Int): List[(String, Long)] = {
    List[(String, Long)]() //Not implemented for log events
  }
  
  def getEventChannels(count: Int): List[(String, Long)] = {
    List[(String, Long)]() //Not implemented for log events
  }
  
  def getStatistics(channel: String, category: Option[String], fromTimestamp: Option[Long], toTimestamp: Option[Long], interval: String): (Long, List[Long]) = {
    (0L, List[Long]()) //Not implemented for log events
  }
  
}
