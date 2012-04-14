package se.aorwall.bam.storage

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.storage.cassandra.DataEventCassandraStorage
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.storage.cassandra.KpiEventCassandraStorage
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import se.aorwall.bam.storage.cassandra.LogEventCassandraStorage
import se.aorwall.bam.storage.cassandra.SimpleProcessEventCassandraStorage
import se.aorwall.bam.storage.cassandra.RequestEventCassandraStorage

/**
 * Not really a test yet. Just doing some write and read tests against
 * a remote Cassandra db
 */
 
@RunWith(classOf[JUnitRunner])
class Test extends FunSuite with Logging {
  
  val conf = ConfigFactory.parseString("""
			akka {
			  bam {
			    storage {
			        
			      implementations {
			      }
			
			      storage-bindings {
			      }
			    
			      cassandra {
			        hostname = "localhost"
			        port = 9160
			        clustername = "TestCluster"
			        keyspace = "Bam"
			      }
			    }
			  }
			}
  """)
  
  val system = ActorSystem("EventStorageSpec", conf)

  test("Data event"){

    val data1 = new DataEvent("irc", Some("#scala"), "329380921316", System.currentTimeMillis, "message1")
    val data2 = new DataEvent("irc", Some("#cassandra"), "329380921317", System.currentTimeMillis, "message2")
    val dataStorage = new DataEventCassandraStorage(system)    
    dataStorage.storeEvent(data1)
    dataStorage.storeEvent(data2)
    info("DataEventStorage 1: " + dataStorage.getEvents("irc", None, None, None, 10, 0))
    info("DataEventStorage 2: " + dataStorage.getEvents("irc", Some("#scala"), None, None, 10, 0))
    
    info("DataEventStorage 2: " + dataStorage.getEventCategories("irc", 10))
    
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

  val log1 = new LogEvent("channel", None, "329380921338", System.currentTimeMillis, "329380921308", "client", "server", Start, "message")
  val log2 = new LogEvent("channel", None,"329380921339", System.currentTimeMillis, "329380921308", "client", "server", Success, "message")
     
  test("Log event"){
   
    val logStorage = new LogEventCassandraStorage(system)      
    logStorage.storeEvent(log1)
    logStorage.storeEvent(log2)
    info("LogEventStorage: " + logStorage.getEvents("channel", None, None, None, 10, 0))
  }     
  
  val req1 = new RequestEvent("channel", None, "329380921328", System.currentTimeMillis, Some(EventRef(log1)), Some(EventRef(log2)), Start, 10L)
  val req2 = new RequestEvent("channel", None, "329380921329", System.currentTimeMillis+1, Some(EventRef(log1)), Some(EventRef(log1)), Start, 10L)
    
  test("Request event") {
   
    val requestStorage = new RequestEventCassandraStorage(system)    
    requestStorage.storeEvent(req1)
    requestStorage.storeEvent(req2)
    info("RequestEventStorage: " + requestStorage.getEvents("channel", None, None, None, 10, 0))
    
  }
  
  test("Simple process event"){
    val simpleStorage = new SimpleProcessEventCassandraStorage(system)    
    simpleStorage.storeEvent(new SimpleProcessEvent("channel", None, "329380921318", System.currentTimeMillis, List(EventRef(req1), EventRef(req2)), Start, 10L))
    simpleStorage.storeEvent(new SimpleProcessEvent("channel", None, "329380921319", System.currentTimeMillis+1, List(EventRef(req1), EventRef(req2)), Success, 10L))
    info("SimpleProcessEventStorage: " + simpleStorage.getEvents("channel", None, None, None, 10, 0))
    
    //cluster.getConnectionManager().shutdown();
  }

}