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

import scala.collection.JavaConversions._
import com.typesafe.config.Config
import akka.actor._
import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

object CassandraStorageExtension extends ExtensionId[CassandraStorageSettings] with ExtensionIdProvider {
  override def get(system: ActorSystem): CassandraStorageSettings = super.get(system)
  def lookup() = this
  def createExtension(system: ExtendedActorSystem) = new CassandraStorageSettings(system.settings.config)
}

class CassandraStorageSettings(val config: Config) extends Extension {

  import config._

  val Hostname = getString("evactor.storage.cassandra.hostname")
  val Port = getInt("evactor.storage.cassandra.port")
  val Keyspace = getString("evactor.storage.cassandra.keyspace")
  val Clustername = getString("evactor.storage.cassandra.clustername")
  
  val ChannelIndex = getIndex("evactor.storage.cassandra.index.channels")
  val EventTypeIndex = getIndex("evactor.storage.cassandra.index.events")
  
  private[this] def getIndex(path: String): Map[String, List[Set[String]]] =
    if(hasPath(path)){
      getConfig(path).root.keySet.map { k =>
        k -> getConfig("%s.%s".format(path, k)).root.values.map { vs =>
          vs.unwrapped match {
            case s: String => Set(s)
            case l: java.util.ArrayList[String] => l.sortWith(_ < _).toSet 
          }
        }.toList
      }.toMap
    } else {
      Map()
    }
  
}
