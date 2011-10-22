package se.aorwall.logserver.cassandra

import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.service.CassandraHostConfigurator

object CassandraUtil {

  def getKeyspace(clusterName: String, host: String, port: Int, keyspace: String) = {
    val cluster = HFactory.getOrCreateCluster(clusterName, new CassandraHostConfigurator(host + ":" + port))
    HFactory.createKeyspace(keyspace, cluster)
  }
}