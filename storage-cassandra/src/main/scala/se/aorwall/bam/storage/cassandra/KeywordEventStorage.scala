package se.aorwall.bam.storage.cassandra
import akka.actor.ActorSystem
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.factory.HFactory
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.cassandra.serializers.UUIDSerializer
import org.joda.time.DateTime
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.storage.EventStorage


class KeywordEventStorage(system: ActorSystem, cfPrefix: String) extends CassandraStorage (system, cfPrefix) with EventStorage {

   type EventType = KeywordEvent
  
	def this(system: ActorSystem) = this(system, "KeywordEvent")

	override val columnNames = List("name", "id", "timestamp", "keyword", "eventRef")

	override def storeEvent(event: Event): Boolean = event match {
		  
		case keywordEvent: KeywordEvent => {
	
			val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
			val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)
	
			val key = keywordEvent.name + ":" + keywordEvent.keyword
	
			// column family: KeywordEvent
			// row key: keyword name
			// column key: timeuuid
			// column value: keyword value			
			mutator.incrementCounter(event.name, cfPrefix + EVENT_CF, keywordEvent.keyword, 1)
			
			// column family: KeywordEventTimeline
			// row key: keyword name + value
			// column key: event timestamp
			// value: event id		
			mutator.insert(key, cfPrefix + TIMELINE_CF, HFactory.createColumn(timeuuid, keywordEvent.eventRef, UUIDSerializer.get, StringSerializer.get))
	
			// column family: EventCount
			// row key: event name + state + ["year";"month":"day":"hour"]
			// column key: timestamp
			// value: counter
			val time = new DateTime(event.timestamp)
			val count = new java.lang.Long(1L)
			val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
			val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
			val day = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0).toDate.getTime)
			val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
	
			mutator.incrementCounter(key + ":" + YEAR, cfPrefix + COUNT_CF, year, count)
			mutator.incrementCounter(key + ":" + MONTH, cfPrefix + COUNT_CF, month, count)
			mutator.incrementCounter(key + ":" + DAY, cfPrefix + COUNT_CF, day, count)
			mutator.incrementCounter(key + ":" + HOUR, cfPrefix + COUNT_CF, hour, count)
	
			true
		}
		case _ => false
	}
	
	def eventToColumns(event: Event): List[(String, String)] = event match {  
	  case keywordEvent: KeywordEvent => 	  
		("name", event.name) :: ("id", event.id) :: ("timestamp", String.valueOf(event.timestamp)) :: ("keyword", keywordEvent.keyword) :: ("eventRef", keywordEvent.eventRef) :: Nil
	}
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new KeywordEvent(get("name").asInstanceOf[String], 
			 			get("id").asInstanceOf[String],
			 			java.lang.Long.parseLong(get("timestamp")),
			 			get("keyword").asInstanceOf[String],
			 			get("eventRef").asInstanceOf[String])
	}
}
