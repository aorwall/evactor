package se.aorwall.logserver.model

class Statistics (val successful: Long,
                  val internalFailures: Long,
                  val backendFailures: Long,
                  val clientFailures: Long,
                  val timeouts: Long,
                  val averageLatency: Double){

  def +(otherStat: Statistics): Statistics = {
    new Statistics(
      successful + otherStat.successful,
      internalFailures + otherStat.internalFailures,
      backendFailures + otherStat.backendFailures,
      clientFailures + otherStat.clientFailures,
      timeouts + otherStat.timeouts,
      averageLatency + otherStat.averageLatency)
  }

  override def toString() = {
     "successful: " + successful + ", " +
     "internalFailures: " + internalFailures + ", " +
     "backendFailures: " + backendFailures + ", " +
     "clientFailures: " + clientFailures + ", " +
     "timeouts: " + timeouts + ", " +
     "averageLatency: " + averageLatency
  }

}