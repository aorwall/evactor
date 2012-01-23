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
import se.aorwall.bam.model.events.KeywordEvent
//import se.aorwall.bam.storage.cassandra.DataEventStorage

class KeywordEventAPI(system: ActorSystem) extends NettyPlan {
    
  //val storage = new DataEventStorage(system)    
  
  lazy val storage = EventStorageExtension(system).getEventStorage(classOf[KeywordEvent].getName) match {
    case Some(s) => s
    case None => throw new RuntimeException("No storage impl")
  }
       
  def now = System.currentTimeMillis
  
  def intent = {
    case req @ Path(Seg("keyword" :: Nil)) => ResponseString(generate(storage.getEventNames()))
    case req @ Path(Seg("keyword" :: "stats" :: name :: keyword :: Nil)) => ResponseString(generate(storage.readStatistics(decode("%s/%s".format(name, keyword)), Some(0L), Some(now), "day")))
    case req @ Path(Seg("keyword" :: "stats" :: name :: keyword ::interval :: Nil)) => ResponseString(generate(storage.readStatistics(decode("%s/%s".format(name, keyword)), Some(0L), Some(now), interval)))
    case req @ Path(Seg("keyword" :: "stats" :: name :: keyword ::interval :: from :: Nil)) => ResponseString(generate(storage.readStatistics(decode("%s/%s".format(name, keyword)), Some(from.toLong), Some(now), interval)))
    case req @ Path(Seg("keyword" :: "stats" :: name :: keyword ::interval :: from :: to :: Nil)) => ResponseString(generate(storage.readStatistics(decode("%s/%s".format(name, keyword)), Some(from.toLong), Some(to.toLong), interval)))
    case _ => ResponseString("Couldn't handle request")
  }
  
  
}
