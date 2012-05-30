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

import akka.actor.Props
import akka.actor.ActorLogging
import org.evactor.monitor.Monitored
import akka.actor.ActorRef
import scala.collection.mutable.HashMap
import org.evactor.model.Timeout
import org.evactor.model.Message

/** 
 * Trait used by processors that handles it own children processors (sub processors)
 */
trait HasChildren extends Processor with Monitored with ActorLogging {
  
  private val children = new HashMap[String, ActorRef] 
  
  override def receive = {
    case Terminated(id) => handleTerminated(id) 
    case msg => super.receive(msg)
  }
  
  protected def handleTerminated(childId: String) {
    log.debug("Removing actor with id {}", childId)
    children.remove(childId)
    addGauge("children", children.size)    
  }
  
  protected def createSubProcessor(id: String): SubProcessor
  
  private[this] def createNewActor(id: String): ActorRef = {
      val newActor = context.actorOf(Props(createSubProcessor(id)))
      children.put(id, newActor)
      addGauge("children", children.size)
      newActor 
  }
  
  protected def getSubProcessor(id: String): ActorRef = 
    children.getOrElseUpdate(id, createNewActor(id))
  
  abstract override def preStart {
    super.preStart()
    addGauge("children", 0)
  }
  
  abstract override def postStop {
    super.postStop()
    removeGauge("children")
  }
  
}

case class Terminated(id: String) 