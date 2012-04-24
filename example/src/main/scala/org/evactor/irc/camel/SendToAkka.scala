/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.irc.camel

import akka.actor.ActorRef
import org.apache.camel.Body
import org.evactor.model.events.DataEvent
import org.apache.camel.Exchange
import org.apache.camel.Headers
import java.util.Map;
import grizzled.slf4j.Logging

class SendToAkka (actor: ActorRef) extends Logging {

	def send(@Headers headers: Map[String, String], @Body body: String, exchange: Exchange){

		val jsonMessage = "{\"nick\": \"" + headers.get("irc.user.nick")+ "\", \"message\": \""+ body + "\"}";		
		val dataEvent = new DataEvent("irc/"+headers.get("irc.target"), None, headers.get("irc.user.nick")+":"+System.currentTimeMillis(), System.currentTimeMillis(), jsonMessage);
		debug(dataEvent)
		actor ! dataEvent;
	}

}