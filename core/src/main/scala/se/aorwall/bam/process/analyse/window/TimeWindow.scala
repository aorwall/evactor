package se.aorwall.bam.process.analyse.window

trait TimeWindow extends Window {

  val timeframe: Long

  override protected[analyse] def getInactive(activities: Map[Long, S]): Map[Long, S] = {
    activities.takeWhile( _._1 < System.currentTimeMillis - timeframe )
  }

  // Scheduler.schedule(self, new Timeout, timeframe, timeframe, TimeUnit.MILLISECONDS)

  //TODO: Scheduler.shutdown

}

case class TimeWindowConf(timeframe: Long) extends WindowConf {

}