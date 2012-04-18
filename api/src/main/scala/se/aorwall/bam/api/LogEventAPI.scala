package se.aorwall.bam.api

import akka.actor.ActorSystem
import com.codahale.jerkson.Json.generate
import se.aorwall.bam.model.events.DataEvent
import se.aorwall.bam.storage.EventStorageExtension
import se.aorwall.bam.storage.Storage
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ResponseString
import org.codehaus.jackson.map.ObjectMapper
import java.net.URLEncoder
import java.net.URLDecoder
import unfiltered.request.Params
import unfiltered.response.BadRequest
import unfiltered.response.NotFound
import se.aorwall.bam.model.events.RequestEvent
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event
import scala.io.Source
import se.aorwall.bam.model.State
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.storage.EventStorage
import unfiltered.response.ResponseFunction
import org.jboss.netty.handler.codec.http.HttpResponse

class LogEventAPI(val system: ActorSystem) extends EventAPI {
    
  lazy val storage = EventStorageExtension(system).getEventStorage(classOf[LogEvent].getName) match {
    case Some(s: EventStorage) => s
    case None => throw new RuntimeException("No storage impl")
  }
     
  override def doRequest(
      path: Seq[String], 
      params: Map[String, Seq[String]]): ResponseFunction[HttpResponse] = path match {
    case "event" :: id :: Nil => getEvent(id) 
    case _ => ResponseString("Couldn't handle request")
  }
  
  override implicit protected[api] def toMap(e: Event): Map[String, Any] = e match { 
    case log: LogEvent => Map ("id" -> log.id, 
         "timestamp" -> log.timestamp,
         "correlationId" -> log.correlationId,
         "client" -> log.client,
         "server" -> log.server,
         "state" -> log.state.toString,
         "message" -> log.message)   
  }
}

