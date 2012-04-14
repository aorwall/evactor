package se.aorwall.bam.api

import akka.actor.ActorSystem
import com.codahale.jerkson.Json._
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

class DataEventAPI(system: ActorSystem) extends NettyPlan {
    
  lazy val storage = EventStorageExtension(system).getEventStorage(classOf[DataEvent].getName) match {
    case Some(s) => s
    case None => throw new RuntimeException("No storage impl")
  }
     
  def now = System.currentTimeMillis
    
  def intent = {
    case req @ Path(Seg("data" :: "categories" :: channel :: Nil)) => try {
   	   val Params(params) = req      
   	 	ResponseString(generate(storage.getEventCategories(channel, getCount(params.get("count"), 20))))
    	} catch { case _ => BadRequest }
    case req @ Path(Seg("data" :: "stats" :: channel :: Nil)) => try {
	      val Params(params) = req       
	      //TODO: Extract parameters
	   	ResponseString(generate(storage.getStatistics(channel, None, Some(0L), Some(now), "hour")))
      } catch { case _ => BadRequest }
    case req @ Path(Seg("data" :: "stats" :: channel :: category :: Nil)) => try {
      val Params(params) = req       
      //TODO: Extract parameters
	   	ResponseString(generate(storage.getStatistics(channel, Some(category), Some(0L), Some(now), "hour")))
	    } catch { case _ => BadRequest }
    case req @ Path(Seg("data" :: "events" :: channel :: Nil)) => try {
   	 	val Params(params) = req       
   	 	//TODO: Extract parameters
   	 	ResponseString(generate(storage.getEvents(channel, None, None, None, 10, 0)))
    	} catch { case _ => BadRequest }
    case req @ Path(Seg("data" :: "events" :: channel :: category :: Nil)) => try {
   	 	val Params(params) = req       
   	 	//TODO: Extract parameters
   	 	ResponseString(generate(storage.getEvents(channel, Some(category), None, None, 10, 0)))
    	} catch { case _ => BadRequest }
   // case _ => ResponseString("Couldn't handle request (data)")
  }
  
  private def getPath(pathlist: List[String]): Option[String] = 
    if (pathlist.size == 0) None
    else Some(decode(pathlist.mkString("/")))
  
  private def getCount(count: Option[Seq[String]], default: Int): Int = count match {
    case Some(s) => s.mkString.toInt 
    case None => default
  }
}

