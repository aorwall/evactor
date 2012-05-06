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
package org.evactor.twitter.listener

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.commons.httpclient.util.EncodingUtil
import org.apache.commons.codec.binary.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import akka.actor.ActorRef
import org.evactor.listen.Listener
import akka.actor.ActorLogging
import akka.util.duration._
import java.io.Reader
import org.evactor.listen.ListenerConfiguration
import com.twitter.ostrich.stats.Stats

class TwitterListenerConfig(url: String, credentials: String) extends ListenerConfiguration {
  
  def listener(sendTo: ActorRef) = new TwitterListener(url, credentials, sendTo)
  
}

class TwitterListener(url: String, credentials: String, sendTo: ActorRef) extends Listener with ActorLogging {
  
  lazy val stream = connect(url, credentials)
  
  def receive = {
    case "" => read()
    case msg => log.debug("can't handle {}", msg)
  }

  private[this] def read(){
    
    val inputLine = stream.readLine()
    
    if (inputLine == null) {
      Stats.incr("twitterlistener:null")
      log.debug("inputline is null")
      Thread.sleep(25)
    } else if (inputLine.trim.size == 0) {
      Stats.incr("twitterlistener:empty")
      log.debug("inputline is empty")
      Thread.sleep(25)
    } else {
      Stats.incr("twitterlistener:status")
      log.debug(inputLine)
      sendTo ! inputLine
    }
    
    context.self ! ""
    
  }
  
  private[this] def connect(url: String, credentials: String): BufferedReader = {
    
    if(url == null)
      throw new IllegalArgumentException("No url provided")
    
    if(credentials == null || ":" == credentials)
      throw new IllegalArgumentException("No credentials provided")
    
    val client = new DefaultHttpClient();
    val method = new HttpGet(url);
    val encoded = EncodingUtil.getAsciiString(Base64.encodeBase64(EncodingUtil.getAsciiBytes(credentials)))
    method.setHeader("Authorization", "Basic " + encoded);
    method.setHeader("Content-Type", "text/xml;charset=UTF-8") 
    
    val response = client.execute(method);
    
    if(response.getStatusLine.getStatusCode != 200){
      throw new RuntimeException("Couldn't connect to the Twitter stream API, status returned: %s".format(response.getStatusLine))
    }
    
    val entity = response.getEntity
    new BufferedReader(new InputStreamReader(entity.getContent))
  }
  
  override def preStart() {
    context.self ! ""
  }
  
  override def postStop() {
  }

}