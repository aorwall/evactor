package se.aorwall.bam.irc.camel

import akka.actor.ActorRef
import org.apache.camel.Body
import se.aorwall.bam.model.events.DataEvent
import org.apache.camel.Exchange
import org.apache.camel.Headers
import java.util.Map;
import grizzled.slf4j.Logging

class SendToAkka (actor: ActorRef) extends Logging {

	def send(@Headers headers: Map[String, String], @Body body: String, exchange: Exchange){

		val jsonMessage = "{\"nick\": \"" + headers.get("irc.user.nick")+ "\", \"message\": \""+ body + "\"}";		
		val dataEvent = new DataEvent("irc/"+headers.get("irc.target"), headers.get("irc.user.nick")+":"+System.currentTimeMillis(), System.currentTimeMillis(), jsonMessage);
		debug(dataEvent)
		actor ! dataEvent;
	}

}