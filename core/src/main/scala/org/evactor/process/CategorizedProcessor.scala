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
import org.evactor.ConfigurationException
import scala.collection.immutable.HashSet
import scala.collection.mutable.HashMap
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Actor
import java.util.UUID

/**
 * Creates sub processors for each category. To use this processor type, an implementation of
 * SubProcessor must be created.
 * 
 * Categorization can be done in three ways:
 * NoCategorization: All events will be sent to the same sub processor
 * OneAndOne: Send the event to a sub processor for each category
 * AllInOne: Send the event to a sub processor for all categories
 * 
 */
abstract class CategorizedProcessor (
    override val subscriptions: List[Subscription],
    val categorization: Categorization)
  extends Processor(subscriptions) 
  with Subscriber 
  with Monitored
  with ActorLogging {

  private val children = new HashMap[Set[String], ActorRef] 

  lazy private[this] val sendEvent: (Message) => Unit = categorization match {
    case NoCategorization => sendNonCategorized
    case OneAndOne => sendOneAndOne
    case AllInOne => sendAllToOne
  }
  
  final override def receive = {
    case msg: Message => incr("process"); sendEvent(msg) 
    case Terminated(supervised) => handleTerminated(supervised)
    case msg => log.warning("Can't handle {}", msg)
  }
  
  private[this] def sendNonCategorized(msg: Message) {
    getCategoryProcessor(Set()) ! msg.event
  }
  
  private[this] def sendOneAndOne(msg: Message) {
    msg.categories foreach { category => getCategoryProcessor(Set(category)) ! msg.event }  
  }
  
  private[this] def sendAllToOne(msg: Message) {
    getCategoryProcessor(msg.categories) ! msg.event  
  }
  
  override def preStart = {
    // Start up one sub processor to handle all events if no categorization is used
    categorization match {
      case NoCategorization => {
        getCategoryProcessor(Set())  
      }
    }
    addGauge("children", children.size)
    super.preStart() 
  }
  
  override def postStop {
    removeGauge("children")
    super.postStop()
  }
  
  protected def createCategoryProcessor(categories: Set[String]): CategoryProcessor
  
  private[this] def createNewActor(categories: Set[String]): ActorRef = {
      val newActor = context.actorOf(Props(createCategoryProcessor(categories)))
      children.put(categories, newActor)
      addGauge("children", children.size)
      newActor 
  }
  
  protected def getCategoryProcessor(categories: Set[String]): ActorRef = 
    children.getOrElseUpdate(categories, createNewActor(categories))
  
  protected def handleTerminated(categories: Set[String]) {
    log.debug("Removing actor with categories {}", categories)
    children.remove(categories)
    addGauge("children", children.size)    
  }
  
  def process(event: Event) {}
}

/**
 * Sent from the sub processor to the parent when the sub processor terminates
 */
case class Terminated(categories: Set[String]) 

/**
 * Specifies the categorization strategy
 */
object Categorization {
  
  val NO_CATEGORIZATION = "NoCategorization"
  val ONE_AND_ONE = "OneAndOne"
  val ALL_IN_ONE = "AllInOne"
  
  def apply(cat: String): Categorization = cat match {
    case NO_CATEGORIZATION => NoCategorization
    case ONE_AND_ONE => OneAndOne
    case ALL_IN_ONE => AllInOne
    case _ => throw new ConfigurationException("Couldn't create a categorization instance with argument: " + cat)
  }
  
}

sealed trait Categorization { 
  def name: String 
  override def toString = name
}

case object NoCategorization extends Categorization { val name = "NoCategorization" }
case object OneAndOne extends Categorization { val name = "OneAndOne" }
case object AllInOne extends Categorization { val name = "AllInOne" }

/**
 * Processor used as a child to a categorized processor. It can't subscribe to channels and
 * will only receive events (not encapsulated in a Message object) from it's parent
 * processor.
 * 
 * A CategoryProcessor isn't meant to be a persistent processor but should able to close itself down
 * when it's done.
 */
abstract class CategoryProcessor(
    val categories: Set[String]) 
  extends Actor 
  with ActorLogging {
  
  final override def receive = {
    case event:Event => process(event)
    case Timeout => timeout()
    case msg => log.warning("Can't handle {}", msg)
  }

  protected def process(event: Event)
  
  protected def timeout() = {}

  
  /**
   * Inform parent when stopped. Would like to use death watch here but
   * waiting for http://www.assembla.com/spaces/akka/tickets/1901
   */
  override def postStop = {
     context.parent ! new Terminated(categories)
  }
  
  def uuid = UUID.randomUUID.toString
  def currentTime = System.currentTimeMillis
}


/**
 * Set category to specified categories if no categories found in publication. 
 *
 * TODO: Better solution here...
 */
class CategoryPublication(publication: Publication, categories: Set[String]) extends Publication{
  
  def channel(event: Event) = publication.channel(event)
  
  def categories(event: Event) = {
    
    val cs = publication.categories(event)
    
    if(cs == Set()){
      categories
    } else {
      cs
    }
    
  }
  
}
