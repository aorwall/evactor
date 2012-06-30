package org.evactor.storage.cassandra

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CassandraStorageExtensionSpec extends WordSpec with MustMatchers with ShouldMatchers {

  val conf = ConfigFactory.parseString("""

        evactor {
          storage {
              
            cassandra {

              index {
                channels {
                  test_channel {
                    idx1 = fieldName
                    idx2 = [field1, field2]
                  }
      
                  test_channel2 {
                    idx1 = [foo, abc, def]
                  }
                }
      
                events {
                  LogEvent {
                    idx1 = a
                    idx2 = [b, c]
                  }
                }
              }

              hostname = "localhost"
              port = 9160
              clustername = "TestCluster"
              keyspace = "Evactor"
            }
          }
        }

  """)
  
  
  "CassandraStorageSettings" must {
  
    "extract configuration" in {
      val settings = new CassandraStorageSettings(conf)
      
      println(settings.ChannelIndex)
      println(settings.EventTypeIndex)
      
    }
  }
}