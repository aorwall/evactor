package se.aorwall.logserver.storage

import se.aorwall.logserver.model.{Activity, Log}
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.Keyspace
import me.prettyprint.cassandra.serializers.{StringSerializer, ObjectSerializer, UUIDSerializer}
import grizzled.slf4j.Logging
import scala.collection.JavaConversions._

class CassandraStorage(keyspace: Keyspace) extends LogStorage with Logging {

  val LOG_CF = "Log"
  val ACTIVITY_CF = "Activity"

  def storeLog(activityId: String, log: Log): Unit = {
     val mutator = HFactory.createMutator(keyspace, StringSerializer.get);
     val logUuid = TimeUUIDUtils.getTimeUUID(log.timestamp)

     // column family: log
     // row key: activity id
     // column key: log timestamp
     // column value: log object
     mutator.insert(activityId, LOG_CF, HFactory.createColumn(logUuid, log, UUIDSerializer.get, ObjectSerializer.get))
  }

  def readLogs(activityId: String): List[Log] = {
    HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, ObjectSerializer.get)
            .setColumnFamily(LOG_CF).setKey(activityId).setRange(null, null, false, 100) //TODO: What if an activity has more than 100 logs?
            .execute()
            .get()
            .getColumns()
            .map { _.getValue match {
                    case l:Log => l
                 }} toList
  }

  def removeActivity(activityId: String): Unit = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    mutator.delete(activityId, LOG_CF, null, StringSerializer.get) // does this really delete the row key?
  }

  def storeActivity(activity: Activity): Unit = {
    val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
    val timeuuid = TimeUUIDUtils.getTimeUUID(activity.endTimestamp)

    // column family: Activites
    // row key: process id
    // column key: end timestamp?
    // value: activity object
    // TODO: Add expiration time if found in business process configuration
    mutator.insert(activity.processId, ACTIVITY_CF, HFactory.createColumn(timeuuid, activity, UUIDSerializer.get, ObjectSerializer.get))

    // TODO: also save in state column family or something...?

    // TODO: ...and count
  }

  def readActivities(processId: String, fromTimestamp: Option[Long], count: Int ): List[Activity] = {
    val timeuuid = fromTimestamp match {
      case Some(from) =>  TimeUUIDUtils.getTimeUUID(from)
      case None => null
    }

    HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, ObjectSerializer.get)
            .setColumnFamily(ACTIVITY_CF)
            .setKey(processId)
            .setRange(null, timeuuid, true, count)
            .execute()
            .get()
            .getColumns()
            .map { _.getValue match {
                    case a:Activity => a
                 }} toList
  }

  def activityExists(activityId: String): Boolean = {
     HFactory.createSliceQuery(keyspace, StringSerializer.get, UUIDSerializer.get, ObjectSerializer.get)
            .setColumnFamily(LOG_CF)
            .setKey(activityId)
            .setRange(null, null, false, 1)
            .execute()
            .get()
            .getColumns()
            .size() > 0
  }
}