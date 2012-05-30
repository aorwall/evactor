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
package org.evactor.process

import org.evactor.model.events.Event
import org.evactor.model.Message
import org.evactor.model.Timeout
import org.evactor.monitor.Monitored
import org.evactor.subscribe.Subscriber
import org.evactor.subscribe.Subscription
import akka.actor.ActorLogging
import org.evactor.publish.Publication

/**
 * Creates sub processors for each category. To use this processor type, an implementation of
 * SubProcessor must be created.
 */
abstract class CategoryProcessor (
    override val subscriptions: List[Subscription],
    val categorize: Boolean)
  extends Processor(subscriptions) with HasChildren
  with Subscriber 
  with Monitored
  with ActorLogging {
  
  final override def receive = {
    case Message(channel, categories, event) => {
      incr("process"); 
      if(categorize){
        categories foreach { category => getSubProcessor(category) ! event }  
      } else {
        getSubProcessor(channel) ! event
      }
      
    }
    case Terminated(supervised) => handleTerminated(supervised)
    case msg => log.warning("Can't handle {}", msg)
  }
  
  override def preStart = {
    
    // Start up actors for all subscribed channels if "categorized" isn't set
    if(!categorize){
      for( Subscription(channel, _) <- subscriptions  ){
        if(channel.isDefined){
          getSubProcessor(channel.get)  
        }
      }
    }
    
    super.preStart()
    
  }
  
  def process(event: Event) {}
}

/**
 * Set category to "id" if no categories found in publication. 
 *
 * TODO: Better solution here...
 */
class CategoryPublication(publication: Publication, category: String) extends Publication{
  
  def channel(event: Event) = publication.channel(event)
  
  def categories(event: Event) = {
    
    val categories = publication.categories(event)
    
    if(categories == Set()){
      Set(category)
    } else {
      categories
    }
    
  }
  
}
