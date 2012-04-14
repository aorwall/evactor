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
import se.aorwall.bam.model.events.KpiEvent
import se.aorwall.bam.storage.KpiEventStorage

class KpiEventAPI(system: ActorSystem) extends NettyPlan {
    
  lazy val storage = EventStorageExtension(system).getEventStorage(classOf[KpiEvent].getName) match {
    case Some(s: KpiEventStorage) => s
    case None => throw new RuntimeException("No storage impl")
  }
     
  def now = System.currentTimeMillis
    
  def intent = {
    case req @ Path(Seg("kpi" :: "categories" :: channel :: Nil)) => try {
   	   val Params(params) = req      
   	 	ResponseString(generate(storage.getEventCategories(channel, getCount(params.get("count"), 100))))
    	} catch { case _ => BadRequest }

    case req @ Path(Seg("kpi" :: "stats" :: channel :: Nil)) => try {
	      val Params(params) = req       
	      //TODO: Extract parameters
	   	ResponseString(generate(storage.getStatistics(channel, None, Some(0L), Some(now), "hour")))
      } catch { case _ => BadRequest }   
    case req @ Path(Seg("kpi" :: "stats" :: channel :: category :: Nil)) => try {
	      val Params(params) = req       
	      //TODO: Extract parameters
	   	ResponseString(generate(storage.getStatistics(channel, Some(category), Some(0L), Some(now), "hour")))
      } catch { case _ => BadRequest }         
    case req @ Path(Seg("kpi" :: "avg" :: channel :: Nil)) => try {
	      val Params(params) = req       
	      //TODO: Extract parameters
	      val avg = average(storage.getSumStatistics(channel, None, Some(0L), Some(now), "hour"))
	   	  ResponseString(generate(avg))
      } catch { case _ => BadRequest }
    case req @ Path(Seg("kpi" :: "avg" :: channel :: category :: Nil)) => try {
	      val Params(params) = req       
	      //TODO: Extract parameters
	      val avg = average(storage.getSumStatistics(channel, Some(category), Some(0L), Some(now), "hour"))
	   	  ResponseString(generate(avg))
      } catch { case _ => BadRequest }
    case req @ Path(Seg("kpi" :: "events" :: channel :: Nil)) =>  try {
   	 	val Params(params) = req       
   	 	//TODO: Extract parameters
   	 	ResponseString(generate(storage.getEvents(channel, None, None, None, 10, 0)))
    	} catch { case _ => BadRequest }
    case req @ Path(Seg("kpi" :: "events" :: channel :: category :: Nil)) =>  try {
   	 	val Params(params) = req       
   	 	//TODO: Extract parameters
   	 	ResponseString(generate(storage.getEvents(channel, Some(category), None, None, 10, 0)))
    	} catch { case _ => BadRequest }
    case _ => ResponseString("Couldn't handle request (data)")
  }
  
  protected def average ( sum: (Long, List[(Long, Double)])) = (sum._1, sum._2.map { 
	        case (x,y) => if(x > 0) y/x
	        					 else 0
	      })
  
  private def getPath(pathlist: List[String]): Option[String] = 
    if (pathlist.size == 0) None
    else Some(decode(pathlist.mkString("/")))
  
  private def getCount(count: Option[Seq[String]], default: Int): Int = count match {
    case Some(s) => s.mkString.toInt 
    case None => default
  }
}

