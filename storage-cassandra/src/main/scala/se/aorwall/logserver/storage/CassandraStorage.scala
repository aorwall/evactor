package se.aorwall.logserver.storage

import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.Keyspace
import me.prettyprint.cassandra.serializers.{StringSerializer, ObjectSerializer, UUIDSerializer, LongSerializer}
import grizzled.slf4j.Logging
import scala.collection.JavaConversions._
import org.joda.time._
import se.aorwall.logserver.model.{State, Statistics, Activity, Log}

class CassandraStorage(keyspace: Keyspace) extends LogStorage with Logging {

  val LOG_CF = "Log"
  val ACTIVITY_TIMELINE_CF = "ActivityTimeline"
  val ACTIVITY_CF = "Activity"
  val ACTIVITY_STATE_CF = "ActivityState"
  val ACTIVITY_COUNT_CF = "ActivityCount"

  val HOUR = "hour"
  val DAY = "day"
  val MONTH = "month"
  val YEAR = "year"

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
    val time = new DateTime(activity.endTimestamp)
    val count = new java.lang.Long(1L)
    val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
    val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
    val day = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0).toDate.getTime)
    val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + YEAR, ACTIVITY_COUNT_CF, year, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + MONTH, ACTIVITY_COUNT_CF, month, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + DAY, ACTIVITY_COUNT_CF, day, count)
    mutator.incrementCounter(activity.processId + ":" + activity.state + ":" + HOUR, ACTIVITY_COUNT_CF, hour, count)
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

  /**
   * Read statistics within a time span from fromTimestamp to toTimestamp
   */
  def readStatistics(processId: String, fromTimestamp: Option[Long], toTimestamp: Option[Long]): Statistics =
    (fromTimestamp, toTimestamp) match {
      case (None, None) => readStatisticsFromInterval(processId, new DateTime(0), new DateTime(System.currentTimeMillis))
      case (Some(from), None) => readStatisticsFromInterval(processId, new DateTime(from), new DateTime(System.currentTimeMillis))
      case (None, Some(to)) => throw new IllegalArgumentException("Reading statistics with just a toTimestamp provided isn't implemented yet") //TODO
      case (Some(from), Some(to)) => readStatisticsFromInterval(processId, new DateTime(from), new DateTime(to))
    }

  def readStatisticsFromInterval(processId: String, from: DateTime, to: DateTime): Statistics = {

    // nollställ millisekunder och sekunder

    lazy val readStat: (DateTime => Statistics) = f => {

      val readFromDb = readStatisticsFromDb(processId, toMillis(f)) _

      val period = new Period(f, to)

      // TODO: Read year if to >= current time and from < current year

      // TODO: Check seconds
      if(f.get(DateTimeFieldType.minuteOfHour()) > 0 && period.getMinutes > 0){
        val nextFrom = f.plusMinutes(60-f.get(DateTimeFieldType.minuteOfHour()))
        readFromDb(None, toMillis(nextFrom)) + readStat(nextFrom)
      } else if(f.get(DateTimeFieldType.hourOfDay()) > 0 && period.getHours > 0){
        val nextFrom = f.plusHours(period.getHours)
        readFromDb(Some(HOUR), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (f.get(DateTimeFieldType.dayOfMonth()) > 1 && new Duration(f, to).getStandardDays > 0) {
        val nextFrom = f.plusHours(new Duration(f, to).getStandardDays.toInt)
        readFromDb(Some(DAY), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (f.get(DateTimeFieldType.monthOfYear()) > 1 && period.getMonths > 0) {
        val nextFrom = f.plusMonths(period.getMonths)
        readFromDb(Some(MONTH), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (period.getYears > 0) {
        val nextFrom = f.plusYears(period.getYears)
        readFromDb(Some(YEAR), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (period.getMonths > 0) {
        val nextFrom = f.plusMonths(period.getMonths)
        readFromDb(Some(MONTH), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (period.getDays > 0) {
        val nextFrom = f.plusDays(period.getDays)
        readFromDb(Some(DAY), toMillis(nextFrom)) + readStat(nextFrom)
      } else if (period.getHours > 0) {
        val nextFrom = f.plusHours(period.getHours)
        readFromDb(Some(HOUR), toMillis(nextFrom)) + readStat(nextFrom)
      } else {
        readFromDb(None, toMillis(to))
      }
    }

    readStat(from)
  }

  def toMillis(date: DateTime) = date.toDate.getTime

  def readStatisticsFromDb(processId: String, from: Long)(dateProperty: Option[String], to: Long) =
    new Statistics(
            readStatisticsCountFromDb(processId, State.SUCCESS, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.INTERNAL_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.BACKEND_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.CLIENT_FAILURE, dateProperty, from, to),
            readStatisticsCountFromDb(processId, State.TIMEOUT, dateProperty, from, to),
            0.0) //TODO: Fix average latency

  def readStatisticsCountFromDb(processId: String, state: Int, dateProperty: Option[String], from: Long, to: Long): Long =
    dateProperty match {
      case Some(prop) => HFactory.createSliceQuery(keyspace, StringSerializer.get, LongSerializer.get, LongSerializer.get)
              .setColumnFamily(ACTIVITY_COUNT_CF)
              .setKey(processId + ":" + state + ":" + prop)
              .setRange(from, to, true, 1000)
              .execute()
              .get
              .getColumns
              .map{_.getValue match {
                  case l: java.lang.Long => l.longValue
                  case _ => 0L
              }}.sum
      case None =>  HFactory.createSliceQuery(keyspace, StringSerializer.get, LongSerializer.get, LongSerializer.get)
              .setColumnFamily(ACTIVITY_STATE_CF)
              .setKey(processId + ":" + state)
              .setRange(from, to, true, 1000)
              .execute()
              .get
              .getColumns.size
  }
}