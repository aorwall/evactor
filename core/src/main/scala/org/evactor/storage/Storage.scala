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
package org.evactor.storage

import java.util.Date
import akka.actor.Actor
import akka.dispatch.MailboxType
import org.evactor.model.events.Event
import org.evactor.model.events.SimpleProcessEvent
import akka.actor.ActorLogging
//import com.twitter.ostrich.stats.Stats

trait Storage extends Actor with ActorLogging {

  val storage = EventStorageExtension(context.system)

  /**
   * Store an event and returns true if successful.
   */
  def storeEvent(event: Event): Unit = storage.getEventStorage match {
//    case Some(storageImpl) => Stats.time("store_%s".format(event.getClass.getName)) { storageImpl.storeEvent(event) }
    case Some(storageImpl) => storageImpl.storeEvent(event) 
    case None => log.debug("No storage implementation found") 
  }
  
  def eventExists(event: Event): Boolean = storage.getEventStorage match {
//    case Some(storageImpl) => Stats.time("check_%s".format(event.getClass.getName)) { storageImpl.eventExists(event) }
    case Some(storageImpl) => storageImpl.eventExists(event)
    case None => {
      log.debug("No storage implementation found") 
      false // Return false if no storage implementation is found
    }
  }
}
