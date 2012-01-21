package se.aorwall.logserver.storage

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.storage.cassandra.DataEventStorage
import se.aorwall.bam.storage.cassandra.KeywordEventStorage
import se.aorwall.bam.storage.cassandra.RequestEventStorage
import se.aorwall.bam.storage.cassandra.SimpleProcessEventStorage
import se.aorwall.bam.model.Success
import se.aorwall.bam.storage.cassandra.LogEventStorage

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
    
    val data1 = new DataEvent("name", "329380921306", System.currentTimeMillis, "message1")
    val data2 = new DataEvent("name", "329380921307", System.currentTimeMillis+1, "message2")
    val dataStorage = new DataEventStorage(system)    
    dataStorage.storeEvent(data1)
    dataStorage.storeEvent(data2)
    info("DataEventStorage: " + dataStorage.readEvents("name", None, None, 10, 0))
            
    val keyStorage = new KeywordEventStorage(system)    
    keyStorage.storeEvent(new KeywordEvent("name", "329380921304", System.currentTimeMillis, "key1", Some(EventRef(data1))))
    keyStorage.storeEvent(new KeywordEvent("name", "329380921305", System.currentTimeMillis+1, "key2", Some(EventRef(data2))))
    info("KeywordEventStorage: " + keyStorage.readEvents("name", None, None, 10, 0))
    
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
    
    //cluster.getConnectionManager().shutdown();
  }

}