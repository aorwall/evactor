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

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.EmptyLocalActorRef
import akka.actor.InternalActorRef
import org.evactor.model.events.Event
import org.evactor.model.Message
import org.evactor.model.Timeout
import akka.actor.Props
import akka.actor.Terminated
import scala.collection.mutable.HashMap
import org.evactor.monitor.Monitored

/**
 * Base class for processors. Contains supporting functions for Processors
 * and SubProcessors
 */
abstract class ProcessorBase
  extends Actor
  with ActorLogging {
    
  protected def handleTerminated(actor: ActorRef) {}
  
}

/** 
 * Trait used by processors that handles it own children processors (sub processors)
 */
trait ProcessorWithChildren extends ProcessorBase with Monitored {
  
  private val children = new HashMap[String, ActorRef] 
  private val idMapping = new HashMap[ActorRef, String] // TODO: Fix bidirectional map instead
  
  override protected def handleTerminated(child: ActorRef) {
    val id = idMapping.remove(child)
    
    id match {
      case Some(s) => {
        log.debug("Removing actor with id {}", s)
        children.remove(s)
        addMetric("children", children.size)
      }
      case None => log.warning("No id found for actor {}", child)
    }
  }
  
  protected def createSubProcessor(id: String): SubProcessor
  
  private[this] def createNewActor(id: String): ActorRef = {
      val newActor = context.actorOf(Props(createSubProcessor(id)))
      children.put(id, newActor)
      idMapping.put(newActor, id)
      context.watch(newActor)
      addMetric("children", children.size)
      newActor 
  }
  
  protected def getSubProcessor(id: String): ActorRef = 
    children.getOrElseUpdate(id, createNewActor(id))
  
  abstract override def preStart {
    super.preStart()
    addMetric("children", 0)
  }
  
  abstract override def postStop {
    super.postStop()
    removeMetric("children")
  }
  
}
