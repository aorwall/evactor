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
package org.evactor.twitter.transformer

import java.text.SimpleDateFormat
import java.util.HashMap
import org.evactor.transform.Transformer
import org.evactor.twitter.StatusEvent
import akka.actor.ActorLogging
import akka.actor.ActorRef
import java.util.ArrayList
import scala.collection.JavaConversions._
import java.util.Locale
import org.evactor.monitor.Monitored
import com.fasterxml.jackson.databind.ObjectMapper

class TwitterJsonToStatusEvent(collector: ActorRef)  extends Transformer with Monitored with ActorLogging {

  val format = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH)
  val mapper = new ObjectMapper
    
  def receive = {
    case jsonString: String => transform(jsonString)
    case msg => log.debug("can't handle {}", msg)
  }
  
  def transform(jsonString: String) {
    val map = mapper.readValue(jsonString, classOf[HashMap[String,Any]])
    
    try{
    
      // skip delete status
      if(map.get("delete") == null){
      
        val text = map.get("text").toString
        val id = map.get("id").toString
        val created_at = map.get("created_at").toString
      
        val timestamp = format.parse(created_at).getTime
        //val timestamp = System.currentTimeMillis
      
        val user = map.get("user")
        val entities = map.get("entities")
      
        //log.info(created_at + ": " + timestamp)
        
        val screenName = user match {
          case m: HashMap[String, String] => m.get("screen_name")
          case msg => throw new RuntimeException("No user object found")
        }
      
        val (urlList, hashtagList) = entities match {
          case m: HashMap[String, ArrayList[HashMap[String, String]]] => (m.get("urls"), m.get("hashtags"))
          case msg => log.warning("No entities object found"); (new ArrayList(), new ArrayList())
        }
        
        val urls: List[String] = urlList.map { u => 
          u.get("expanded_url")
        }.toList
       
        val hashtags: List[String] = hashtagList.map { u => 
          u.get("text")
        }.toList
        
        incr("twittertransformer:success")
        collector ! new StatusEvent(id, timestamp, screenName, text, urls, hashtags)
      } else {
        incr("twittertransformer:delete")
      }
    } catch {
      case e => {
        incr("twittertransformer:failure")
        log.warning("couldn't parse {}", jsonString, e)
        throw e
      }
    }
  }
  
  
}