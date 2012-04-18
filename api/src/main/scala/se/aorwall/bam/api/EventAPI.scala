package se.aorwall.bam.api

import akka.actor.ActorSystem
import se.aorwall.bam.storage.EventStorage
import unfiltered.response.ResponseFunction
import org.jboss.netty.handler.codec.http.HttpResponse
import unfiltered.response.ResponseString
import com.codahale.jerkson.Json.generate
import java.net.URLDecoder
import se.aorwall.bam.model.State
import unfiltered.response.BadRequest
import se.aorwall.bam.model.events.Event
import unfiltered.response.NotFound

abstract class EventAPI {

  val system: ActorSystem
  val storage: EventStorage
  
  def doRequest(
      path: Seq[String], 
      params: Map[String, Seq[String]]): ResponseFunction[HttpResponse] = 
    path match {
      case "channels" :: Nil => getChannels(getCount(params.get("count"), 100))
      case "categories" :: channel :: Nil => getCategories(decode(channel), getCount(params.get("count"), 100))
   	  case "stats" :: tail => getStats(tail, params)
   	  case "events" :: tail => getEvents(tail, params)
   	  case "event" :: id :: Nil => getEvent(id) 
  }
  
  protected[api] def getChannels(count: Int): List[Map[String, Any]] = 
    storage.getEventChannels(count)
  
  protected[api] def getCategories(channel: String, count: Int): List[Map[String, Any]] = 
  	storage.getEventCategories(decode(channel), count)

  protected[api] def getStats(path: Seq[String], params: Map[String, Seq[String]]): Map[String, Any] =
    path match {
      case channel :: Nil => storage.getStatistics(decode(channel), None, Some(0L), Some(now), getInterval(params.get("interval")))
   	  case channel :: category :: Nil => storage.getStatistics(decode(channel), Some(decode(category)), Some(0L), Some(now), getInterval(params.get("interval")))
   	  case e => throw new IllegalArgumentException("Illegal stats request: %s".format(e))
  }
  
  protected[api] def getEvents(path: Seq[String], params: Map[String, Seq[String]]): List[Map[String, Any]] = 
    path match {
 	    case channel :: Nil => storage.getEvents(decode(channel), None, None, None, 10, 0)
 	    case channel :: category :: Nil => storage.getEvents(decode(channel), Some(decode(category)), None, None, 10, 0)
 	    case e => throw new IllegalArgumentException("Illegal events request: %s".format(e))
	  }
  	
  protected[api] def getEvent(id: String): Option[Map[String, Any]] = 
    storage.getEvent(id) match {
 	 	  case Some(e: Event) => Some(e)
 	 	  case _ => None
	  }
  
  implicit protected[api] def anyToResponse(any: AnyRef): ResponseFunction[HttpResponse] = any match {
    case None => NotFound
    case _ => ResponseString(generate(any))
  }
  
  implicit protected[api] def toMap(events: List[Event]): List[Map[String, Any]] = events.map(_.toMap)
   
  implicit protected[api] def toMap(event: Event): Map[String, Any] 
  	
  protected[api] def decode(name: String) = {
    URLDecoder.decode(name, "UTF-8")
  }
  
  protected[api] def getCount(count: Option[Seq[String]], default: Int): Int = count match {
    case Some(s) => s.mkString.toInt 
    case None => default
  }
  
  protected[api] def getInterval (interval: Option[Seq[String]]) = interval match {
    case Some( i :: Nil) => i
		case None => "day"
  }
  
  protected[api] def getState(state: Option[Seq[String]]) = 
    state match {
	    case Some( s:: Nil) => Some(State.apply(s))
	    case None => None
  }
  
  implicit protected[api] def toCountMap(list: List[(String, Long)]): List[Map[String, Any]] =
    list.map { (t) => Map("name" -> t._1, "count" -> t._2) }
  
  implicit protected[api] def toStatsMap(stats: (Long, List[Long])): Map[String, Any] = 
    Map ("timestamp" -> stats._1, "stats" -> stats._2)
    
  protected[api] def now = System.currentTimeMillis
  
}