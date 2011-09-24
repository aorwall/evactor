package se.aorwall.logserver.storage

import se.aorwall.logserver.model.{Activity, Log}
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.Keyspace
import me.prettyprint.cassandra.serializers.{StringSerializer, ObjectSerializer, UUIDSerializer, LongSerializer}
import grizzled.slf4j.Logging
import scala.collection.JavaConversions._
import org.joda.time.DateTime

class CassandraStorage(keyspace: Keyspace) extends LogStorage with Logging {

  val LOG_CF = "Log"
  val ACTIVITY_TIMELINE_CF = "ActivityTimeline"
  val ACTIVITY_CF = "Activity"
  val ACTIVITY_STATE_CF = "ActivityState"
  val ACTIVITY_COUNT_CF = "ActivityCount"

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

    // column family: ActivityTimeline
    // row key: process id
    // column key: end timestamp?
    // value: activity object
    // TODO: Add expiration time if found in business process configuration
    mutator.insert(activity.processId, ACTIVITY_TIMELINE_CF, HFactory.createColumn(timeuuid, activity, UUIDSerializer.get, ObjectSerializer.get))

    // column family: Activity
    // row key: process id
    // column key: activityId, ttl: 24 hours
    mutator.insert(activity.processId, ACTIVITY_CF, HFactory.createColumn(activity.activityId, "", 3600*24, StringSerializer.get, StringSerializer.get))

    // column family: ActivityState
    // row key: process id + state
    // column key: end timestamp
    // value: activity id
    mutator.insert(activity.processId + ":" + activity.state, ACTIVITY_STATE_CF, HFactory.createColumn(timeuuid, activity.activityId, UUIDSerializer.get, StringSerializer.get))

    // column family: ActivityCount
    // row key: process id + state + ["year";"month":"day":"hour"]
    // column key: timestamp
    // value: counter
    // TODO: The reason why TimeUUIDUtils is used here is because I can't get LongSerializer to work. Needs to be fixed...
    val time = new DateTime(activity.endTimestamp)
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val day = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0).toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + "year", ACTIVITY_COUNT_CF, year, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + "month", ACTIVITY_COUNT_CF, month, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + "day", ACTIVITY_COUNT_CF, day, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + "hour", ACTIVITY_COUNT_CF, hour, count)
  }

  def readActivities(processId: String, fromTimestamp: Option[Long], count: Int ): List[Activity] = {
    val timeuuid = fromTimestamp match {
      case Some(from) => TimeUUIDUtils.getTimeUUID(from)
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

  def activityExists(processId: String, activityId: String): Boolean = {
     HFactory.createColumnQuery(keyspace, StringSerializer.get, StringSerializer.get, ObjectSerializer.get)
            .setColumnFamily(ACTIVITY_CF)
            .setKey(processId)
            .setName(activityId)
            .execute()
            .get() != null
  }
}