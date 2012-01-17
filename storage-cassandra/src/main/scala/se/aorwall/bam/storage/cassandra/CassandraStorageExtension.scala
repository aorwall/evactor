package se.aorwall.bam.storage.cassandra

import com.typesafe.config.Config
import akka.actor._

object CassandraStorageExtension extends ExtensionId[CassandraStorageSettings] with ExtensionIdProvider {
  override def get(system: ActorSystem): CassandraStorageSettings = super.get(system)
  def lookup() = this
  def createExtension(system: ActorSystemImpl) = new CassandraStorageSettings(system.settings.config)
}

class CassandraStorageSettings(val config: Config) extends Extension {

  import config._

  val Hostname = getString("akka.bam.storage.cassandra.hostname")
  val Port = getInt("akka.bam.storage.cassandra.port")
  val Keyspace = getString("akka.bam.storage.cassandra.keyspace")
  val Clustername = getString("akka.bam.storage.cassandra.clustername")
  
}
