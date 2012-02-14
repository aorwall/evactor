package se.aorwall.bam.train

import java.io.ByteArrayOutputStream
import java.util.HashMap
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.codehaus.jackson.map.ObjectMapper
import java.util.ArrayList
import scala.collection.JavaConversions._
import se.aorwall.bam.model.events.DataEvent
import akka.actor.Actor
import akka.actor.Cancellable
import akka.util.duration._
import grizzled.slf4j.Logging
import akka.actor.ActorLogging
import akka.actor.ActorRef

class GetTrainReports(val collector: ActorRef) extends Actor with ActorLogging {
  
  var cancellable: Option[Cancellable] = None
  
  def receive () = {
    case "run" => process
  }
  
  def process = {
			val request = 
			  """
			  <ORIONML version="1.0"><REQUEST plugin="WOW" version="" locale="SE_sv"><PLUGINML table="LpvTrafiklagen" filter="VerkligTidpunktAnkomst > datetime('now','localtime','-5 minute') AND datetime('now','localtime','1 minute') > VerkligTidpunktAnkomst AND ArAnkomstTag = true" orderby="AnnonseradTidpunktAnkomst" selectcolumns=""></PLUGINML></REQUEST></ORIONML>
			  """
			  
			log.info("fetching trains")  
			  
			val endpointAddress = "http://trafikinfo.trafikverket.se/litcore/orion/orionproxy.ashx"
			val client = new DefaultHttpClient();
			val method = new HttpPost(endpointAddress);
			method.setEntity(new StringEntity(request))
			method.setHeader("Content-Type", "text/xml;charset=UTF-8") 
			
			val response = client.execute(method);

			val mapper = new ObjectMapper

			val out = new ByteArrayOutputStream()
			response.getEntity().writeTo(out)
						
			val map = mapper.readValue(out.toString(), classOf[HashMap[String, Object]])
			
			val trainList = map.get("LpvTrafiklagen") match {
			  case m: HashMap[String, Object] => m.get("Trafiklage")
			  case _ => List()
			}
			
			val eventList = trainList match {
			  case list: ArrayList[Any] => list.map( _ match {
			    case m: HashMap[String, Object] => {			      
			      val trainNo = if( m.containsKey("AnnonseratTagId")) m.get("AnnonseratTagId")
			      				  else m.get("TeknisktTagId")			      
			      new DataEvent("train/arrival", trainNo + "-" + m.get("Utgangsdatum") + "-" + m.get("TrafikplatsSignatur").toString.toLowerCase, System.currentTimeMillis(), mapper.writeValueAsString(m) )
			    }
			    case _ => List()
			  })
			  case hej => List()
			} 

			eventList.foreach{ a => log.debug("sending " + a); collector ! a } 	
	}
	
	override def preStart() {
	  log.info(context.self + " starting")
	  cancellable = Some(context.system.scheduler.schedule(5 seconds, 1 minute, self, "run"))
	} 
	
	override def postStop() {	  
	  log.info(context.self + " stop")
	  cancellable match {
       case Some(c: Cancellable) => c.cancel()
       case _ =>
     }
	}
	
   override def preRestart(cause: Throwable, msg: Option[Any]) {
     log.warning("An exception was thrown", cause)
     cancellable match {
       case Some(c: Cancellable) => c.cancel()
       case _ =>
     }
   }
	  
}
