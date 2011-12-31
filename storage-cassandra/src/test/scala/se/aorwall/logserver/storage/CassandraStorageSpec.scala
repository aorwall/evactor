package se.aorwall.logserver.storage

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.joda.time.DateTime

class CassandraStorageSpec extends WordSpec with MustMatchers  {

  //TODO: Integration test with Cassandra

  "A CassandraStorage implementation" must {

    "store a log object" in {
       (pending)
    }

    "store an activity object" in {
       (pending)
    }

    "read log objects for an activity" in {
       (pending)
    }

    "read activity objects for a process" in {
       (pending)
    }

    "check if an activity exists" in {
       (pending)
    }

    "count statistics between two dates" in {
       val cassandraStorage = new CassandraStorage(null)
       val from = new DateTime(2007, 9, 25, 21, 33)
       val to = new DateTime(2010, 2, 2, 2, 2)

       println(cassandraStorage.readStatisticsFromInterval("processId", from, to))
    }

  }
}
