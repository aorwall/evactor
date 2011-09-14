package se.aorwall.logserver.storage.mock

import se.aorwall.logserver.storage.LogStorage

import grizzled.slf4j.Logging
import collection.mutable.Map
import se.aorwall.logserver.model.{Activity, LogEvent, Log}
import akka.routing._
import akka.actor.TypedActor._
import scala.Option
import akka.actor.{ActorRef, Actor, TypedActor}

class MockStorage extends TypedActor with LogStorage with Logging {

    def storeLogdata(logdata: Log) = info("storeLogdata: " + logdata)

    def storeLogEvent(processId: String, logevent: LogEvent) = info("storeLogEvent: " + processId + ":" + LogEvent)

    def readLogEvents(processId: String, correlationId: String) = Map[String, List[Int]]()

    def removeLogEvents(processId: String, correlationId: String) =  info("removeLogEvents: " + processId + "/" + correlationId)

    def storeActivity(activity: Activity) =  info("storeActivity: " + activity)
}

