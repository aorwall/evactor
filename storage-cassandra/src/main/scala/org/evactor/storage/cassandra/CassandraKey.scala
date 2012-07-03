package org.evactor.storage.cassandra

trait CassandraKey {
  def keyValue: String
}

case class BasicKey  (
    val channel: String,
    val index: Option[Map[String, String]])
  extends CassandraKey{

  def keyValue = channel + { index match {
    case Some(m) => "?" + m.map(p => p._1+"="+p._2).mkString("&")
    case None => ""
  }}
}

case class IndexKey  (
    val channel: String,
    val fields: Iterable[String])
  extends CassandraKey{

  def keyValue = channel + { if(fields.size > 0) "?" + fields.mkString("&") }
  
}

case class StatisticsKey  (
    val key: CassandraKey,
    val interval: String)
  extends CassandraKey{

  def keyValue = key.keyValue + "&interval=" + interval
  
}