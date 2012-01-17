package se.aorwall.logserver.storage

import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit
import org.apache.cassandra.thrift.CassandraDaemon
import org.joda.time.DateTime
import se.aorwall.bam.storage.cassandra.LogEventStorage
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.storage.cassandra.RequestEventStorage
import se.aorwall.bam.model.events.RequestEvent
import se.aorwall.bam.model.Success
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

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
    val storage = new RequestEventStorage(system)    

    storage.storeEvent(new RequestEvent("name", "329380921309", System.currentTimeMillis, None, None, Success, 0L))
    storage.storeEvent(new RequestEvent("name", "329380921310", System.currentTimeMillis, None, None, Success, 0L))
    storage.storeEvent(new RequestEvent("name", "329380921311", System.currentTimeMillis, None, None, Success, 0L))
    
    info(storage.readEvents("name", None, None, 10, 0))
    
    //cluster.getConnectionManager().shutdown();
  }

}