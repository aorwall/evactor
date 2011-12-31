package se.aorwall.logserver.analyse.statement.window

import grizzled.slf4j.Logging
import akka.actor.{Scheduler, Actor}
import java.util.concurrent.TimeUnit

trait TimeWindow extends Window with Logging{

  val timeframe: Long

  override def getInactive(activities: Map[Long, T]): Map[Long, T] = {
     activities.takeWhile( _._1 < System.currentTimeMillis - timeframe )
  }

  // Scheduler.schedule(self, new Timeout, timeframe, timeframe, TimeUnit.MILLISECONDS)


  //TODO: Scheduler.shutdown

}
