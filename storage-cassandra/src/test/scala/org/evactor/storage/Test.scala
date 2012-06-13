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

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import org.evactor.model.events.DataEvent
import org.evactor.model.events.LogEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.events.SimpleProcessEvent
import org.evactor.model.Start
import org.evactor.model.Success
import org.evactor.model.events.KpiEvent
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.evactor.storage.cassandra.CassandraStorage
import akka.actor.ExtendedActorSystem
import org.evactor.model.Message

/**
 * Not really a test yet. Just doing some write and read tests against
 * a remote Cassandra db
 */
 
@RunWith(classOf[JUnitRunner])
class Test extends FunSuite with Logging {
  
  val conf = ConfigFactory.parseString("""

			  evactor {
			    storage {
			        
			      implementations {
			      }
			
			      storage-bindings {
			      }
			    
			      cassandra {
			        hostname = "localhost"
			        port = 9160
			        clustername = "TestCluster"
			        keyspace = "Evactor"
			      }
			    }
			  }

  """)
  
  val system = ActorSystem("EventStorageSpec", conf)
  val storage = new CassandraStorage(system)    

  /*
  test("Data event"){

    val data1 = new DataEvent("irc", Some("#scala"), "329380921316", System.currentTimeMillis, "message1")
    val data2 = new DataEvent("irc", Some("#cassandra"), "329380921317", System.currentTimeMillis, "message2")
//    dataStorage.storeEvent(data1)
//    dataStorage.storeEvent(data2)
    
//    
//    info("DataEventStorage 2: " + dataStorage.getEventCategories("irc", 10))
    println("DataEventStorage 1: " + dataStorage.getEvents("irc", None, None, None, 10, 0))
    println("DataEventStorage 2: " + dataStorage.getEvents("irc", Some("#scala"), None, None, 10, 0))

    //info(dataStorage.getEvent("329380921317"))
    
  }
  
  test ("KPI event"){
    
    val kpi1 = new KpiEvent("channel", None, "329380921316", System.currentTimeMillis, 5.0)
    val kpi2 = new KpiEvent("channel", None, "329380921317", System.currentTimeMillis+1, 15.0)
    val kpi3 = new KpiEvent("channel", None, "329380921318", System.currentTimeMillis+2, 25.0)
    val kpi4 = new KpiEvent("channel", None, "329380921319", System.currentTimeMillis+3, 35.0)
    val kpiStorage = new KpiEventCassandraStorage(system)
    
    kpiStorage.storeEvent(kpi1)
    kpiStorage.storeEvent(kpi2)
    kpiStorage.storeEvent(kpi3)
    kpiStorage.storeEvent(kpi4)
    
    println("KpiEventStorage: " + kpiStorage.getEvents("channel", None, None, None, 10, 0))
    println("KpiEventStorage: " + kpiStorage.getSumStatistics("channel", None, None, None, "hour"))
    
  }
*/
  val log1 = new Message("logs", Set("1", "2", "3"), new LogEvent("329380921338", System.currentTimeMillis, "329380921308", "component", "client", "server", Start, "message"))
  val log2 = new Message("logs", Set("1"), new LogEvent("329380921339", System.currentTimeMillis, "329380921308", "component", "client", "server", Success, "message"))
     
  test("Log event"){
   (pending)
    storage.storeMessage(log1)
    storage.storeMessage(log2)
    println("LogEventStorage 1: " + storage.getEvents("logs", Some("1"), None, None, 10, 0))
    println("LogEventStorage 2: " + storage.getEvents("logs", Some("2"), None, None, 10, 0))
    println("LogEventStorage 3: " + storage.getEvents("logs", Some("3"), None, None, 10, 0))
    println("LogEventStorage: " + storage.getEvents("logs", None, None, None, 10, 0))
  }     
  
  val req1 = new Message("channel2", Set(), new RequestEvent("329380921328", System.currentTimeMillis, "329380921328", "component", Some("329380921338"), Some("329380921338"), Start, 10L))
  val req2 = new Message("channel2", Set(), new RequestEvent("329380921329", System.currentTimeMillis+1, "329380921328", "component", Some("329380921338"), Some("329380921338"), Start, 10L))
    
//  test("Request event") {
//   
//    storage.storeMessage(req1)
//    storage.storeMessage(req2)
//    info("RequestEventStorage: " + storage.getEvents("channel", None, None, None, 10, 0))
//    
//  }
  /*
  test("Simple process event"){
    val simpleStorage = new SimpleProcessEventCassandraStorage(system)    
    simpleStorage.storeEvent(new SimpleProcessEvent("channel", None, "329380921318", System.currentTimeMillis, List(EventRef(req1), EventRef(req2)), Start, 10L))
    simpleStorage.storeEvent(new SimpleProcessEvent("channel", None, "329380921319", System.currentTimeMillis+1, List(EventRef(req1), EventRef(req2)), Success, 10L))
    info("SimpleProcessEventStorage: " + simpleStorage.getEvents("channel", None, None, None, 10, 0))
    
    //cluster.getConnectionManager().shutdown();
  }*/

}