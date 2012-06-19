package org.evactor.transform

import org.evactor.monitor.Monitored
import org.evactor.model.events.LogEvent
import akka.actor.{ActorLogging, ActorRef}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import java.text.SimpleDateFormat
import xml.{XML, Elem}
import org.evactor.model.{Start, Success, State}
import java.util.UUID

/**
 * User: anders
 */

class SimpleJsonToLogEventTransformer(collector: ActorRef)  extends Transformer with Monitored with ActorLogging {
  val jsonSerializer = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
    m
  }

  def receive = {
    case msg: String => {
      log.debug("Recived msg %s for transformation", msg)
      collector ! jsonSerializer.readValue(msg, classOf[LogEvent])
    }
    case msg => log.debug("can't handle {}", msg)
  }
}


class SimpleXmlToLogEventTransformer(collector: ActorRef)  extends Transformer with Monitored with ActorLogging {
  def receive = {
    case msg: String => {
      log.debug("Recived msg %s for transformation", msg)
      val node = XML.loadString(msg)
      val logEntry = node \ "logEntry"
      val runtimeInfo = (value: String) => (logEntry \ "runtimeInfo" \ value)(0) text
      val metadataInfo = (value: String) => (logEntry \ "metadataInfo" \ value)(0) text
      val messageInfo = (value: String) => (logEntry \ "messageInfo" \ value)(0) text
      val payload = (logEntry \ "payload")(0) text
      val timestamp = runtimeInfo("timestamp").reverse.replaceFirst(":", "").reverse
      val state = matchState(messageInfo("message"))
      if (state.isDefined)
        collector ! LogEvent(runtimeInfo("messageId"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(timestamp).getTime, runtimeInfo("businessCorrelationId"), runtimeInfo("componentId"), runtimeInfo("hostIp"), metadataInfo("endpoint"), state.get, payload)
    }
    case msg => log.debug("can't handle {}", msg)
  }

  def matchState(state: String): Option[State] = state.toLowerCase match {
    case "req-out" => Some(Start)
    case "resp-in" => Some(Success)
    case _ => None
  }
}
