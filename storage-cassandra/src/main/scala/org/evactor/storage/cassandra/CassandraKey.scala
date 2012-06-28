package org.evactor.storage.cassandra

trait CassandraKey {
  def keyValue: String
}

case class BasicKey  (
    val channel: String,
    val category: Option[String],
    val index: Option[Map[String, String]])
  extends CassandraKey{

  def keyValue = channel + category.getOrElse("") + index.getOrElse("")
  
}

case class IndexKey  (
    val channel: String,
    val category: Option[String],
    val fields: Iterable[String])
  extends CassandraKey{

  def keyValue = channel + fields
  
}


case class StatisticsKey  (
    val key: CassandraKey,
    val interval: String)
  extends CassandraKey{

  def keyValue = key.keyValue + interval
  
}