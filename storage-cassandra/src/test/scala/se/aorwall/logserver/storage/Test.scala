package se.aorwall.logserver.storage

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

object Test extends Logging {
  
  val conf = ConfigFactory.parseString("""
			akka {
			  bam {
			    storage {
			        
			      implementations {
			        requestEvent = se.aorwall.bam.storage.cassandra.RequestEventStorage
			      }
			
			      storage-bindings {
			        requestEvent = ["se.aorwall.bam.model.events.RequestEvent"]
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
    
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("EventStorageSpec", conf)

    val data1 = new DataEvent("irc", Some("#scala"), "329380921316", System.currentTimeMillis, "message1")
    val data2 = new DataEvent("irc",Some("#scala"), "329380921316", System.currentTimeMillis+1, "message2")
    val dataStorage = new DataEventCassandraStorage(system)    
    dataStorage.storeEvent(data1)
    dataStorage.storeEvent(data2)
    info("DataEventStorage 1: " + dataStorage.getEvents("irc", None, None, None, 10, 0))
    info("DataEventStorage 2: " + dataStorage.getEvents("irc", Some("##skip"), None, None, 10, 0))
    
    info("DataEventStorage 2: " + dataStorage.getEventCategories("irc", 10))
        
    /*
    val kpi1 = new KpiEvent("name", "329380921316", System.currentTimeMillis, 5.0)
    val kpi2 = new KpiEvent("name", "329380921317", System.currentTimeMillis+1, 15.0)
    val kpi3 = new KpiEvent("name", "329380921318", System.currentTimeMillis+2, 25.0)
    val kpi4 = new KpiEvent("name", "329380921319", System.currentTimeMillis+3, 35.0)
    val kpiStorage = new KpiEventCassandraStorage(system)
    
    kpiStorage.storeEvent(kpi1)
    kpiStorage.storeEvent(kpi2)
    kpiStorage.storeEvent(kpi3)
    kpiStorage.storeEvent(kpi4)
    
    println("KpiEventStorage: " + kpiStorage.readStatistics("name", None, None, "hour"))
    println("KpiEventStorage: " + kpiStorage.readSumStatistics("name", None, None, "hour"))
    
    val keyStorage = new KeywordEventCassandraStorage(system)    
    keyStorage.storeEvent(new KeywordEvent("##skip", "329380921310", System.currentTimeMillis, "aa", Some(EventRef(data1))))
    keyStorage.storeEvent(new KeywordEvent("##skip", "329380921311", System.currentTimeMillis+1, "az", Some(EventRef(data2))))
    keyStorage.storeEvent(new KeywordEvent("##skip", "329380921312", System.currentTimeMillis+2, "aÖ", Some(EventRef(data2))))
    info("KeywordEventStorage: " + keyStorage.readEvents("name", None, None, 10, 0))
    info("KeywordEventStorage: " + keyStorage.getKeywords("##skip", 10, Some("A")))
    
    val log1 = new LogEvent("name", "329380921338", System.currentTimeMillis, "329380921308", "client", "server", Start, "message")
    val log2 = new LogEvent("name", "329380921339", System.currentTimeMillis, "329380921308", "client", "server", Success, "message")
    val logStorage = new LogEventStorage(system)      
    logStorage.storeEvent(log1)
    logStorage.storeEvent(log2)
    info("LogEventStorage: " + logStorage.readEvents("name", None, None, 10, 0))
        
    val req1 = new RequestEvent("name", "329380921328", System.currentTimeMillis, Some(EventRef(log1)), Some(EventRef(log2)), Start, 10L)
    val req2 = new RequestEvent("name", "329380921329", System.currentTimeMillis+1, Some(EventRef(log1)), Some(EventRef(log1)), Start, 10L)
    
    val requestStorage = new RequestEventStorage(system)    
    requestStorage.storeEvent(req1)
    requestStorage.storeEvent(req2)
    info("RequestEventStorage: " + requestStorage.readEvents("name", None, None, 10, 0))
    
    val simpleStorage = new SimpleProcessEventStorage(system)    
    simpleStorage.storeEvent(new SimpleProcessEvent("name", "329380921318", System.currentTimeMillis, List(EventRef(req1), EventRef(req2)), Start, 10L))
    simpleStorage.storeEvent(new SimpleProcessEvent("name", "329380921319", System.currentTimeMillis+1, List(EventRef(req1), EventRef(req2)), Success, 10L))
    info("SimpleProcessEventStorage: " + simpleStorage.readEvents("name", None, None, 10, 0))
    */
    //cluster.getConnectionManager().shutdown();
  }

}