package se.aorwall.bam.api

import akka.actor.Actor
import akka.http._
import se.aorwall.bam.storage.LogStorage

/**
 * Get statistics by providing:
 * - processId
 * - start timestamp
 * - end timestamp
 */

class GetStatistics(logStorage: LogStorage) extends Actor {

  def receive = {

    case get: Get => {
      def default(any: Any): String = ""

      val processId = get.getParameterOrElse("processId", default)
      val fromTimestamp = get.getParameterOrElse("from", default)
      val toTimestamp = get.getParameterOrElse("to", default)

      if (processId == "") get.BadRequest("processId must be provided")

      val from = if (fromTimestamp != "") Some(fromTimestamp.toLong)
                 else None

      val to = if (toTimestamp != "") Some(toTimestamp.toLong)
                 else None

      val statistics = logStorage.readStatistics(processId, from, to)
      get.OK(statistics.toString) // TODO: Transform to JSON
    }
    case other: RequestMethod =>
      other.NotAllowed("Invalid method for this endpoint")
  }
}
