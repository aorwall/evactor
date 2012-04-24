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

import com.typesafe.config.Config
import akka.actor._

object CassandraStorageExtension extends ExtensionId[CassandraStorageSettings] with ExtensionIdProvider {
  override def get(system: ActorSystem): CassandraStorageSettings = super.get(system)
  def lookup() = this
  def createExtension(system: ExtendedActorSystem) = new CassandraStorageSettings(system.settings.config)
}

class CassandraStorageSettings(val config: Config) extends Extension {

  import config._

  val Hostname = getString("akka.bam.storage.cassandra.hostname")
  val Port = getInt("akka.bam.storage.cassandra.port")
  val Keyspace = getString("akka.bam.storage.cassandra.keyspace")
  val Clustername = getString("akka.bam.storage.cassandra.clustername")
  
}
