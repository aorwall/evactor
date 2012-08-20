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

import org.evactor.ConfigurationException
import org.evactor.expression.Expression
import org.evactor.model.{Message, Timeout}
import org.evactor.model.events.Event
import org.evactor.monitor.Monitored
import org.evactor.process.alert.Alerter
import org.evactor.process.analyse.count.CountAnalyser
import org.evactor.process.analyse.average.AverageAnalyser
import org.evactor.process.analyse.trend.RegressionAnalyser
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.process.analyse.window.TimeWindow
import org.evactor.process.build.request.RequestBuilder
import org.evactor.process.build.simpleprocess.SimpleProcessBuilder
import org.evactor.process.produce.LogProducer
import org.evactor.process.route.{Filter, Forwarder}
import org.evactor.publish.Publication
import org.evactor.subscribe._
import akka.actor.{ActorLogging, ReflectiveDynamicAccess}
import com.typesafe.config.{Config, ConfigException}
import scala.collection.JavaConversions._
import java.util.UUID
import org.evactor.process.produce.CamelProducer

/**
 * Abstract class all standard processors should extend
 */
abstract class Processor (
    val subscriptions: List[Subscription]) 
  extends Subscriber 
  with Monitored
  with ActorLogging {

  def receive = {
    case Message(_, event) => incr("process"); process(event)
    case Timeout => timeout()
    case msg => log.warning("Can't handle {}", msg)
  }

  protected def process(event: Event)
  
  protected def timeout() = {}
  
  def uuid = UUID.randomUUID.toString
  def currentTime = System.currentTimeMillis

}

/**
 * Build processor from config
 * 
 * TODO: Create some fancy dynamic "convention over configuration" thing instead
 * 
 */
object Processor {

  lazy val dynamicAccess = new ReflectiveDynamicAccess(this.getClass.getClassLoader)
  
  def apply(config: Config): Processor = {
    
    import config._
    
    lazy val sub = Subscriptions(getConfigList("subscriptions").toList) 
    lazy val pub = Publication(getConfig("publication"))
    
    try {
      if(hasPath("type")){
        getString("type") match{
          case "countAnalyser" => new CountAnalyser(sub, pub, Categorization(getConfig("categorization")), getMilliseconds("timeframe"))
          case "regressionAnalyser" => new RegressionAnalyser(sub, pub, Categorization(getConfig("categorization")), getLong("minSize"), getMilliseconds("timeframe"))
          case "filter" => new Filter(sub, pub, Expression(getConfig("expression")), getBoolean("accept"))
          case "forwarder" => new Forwarder(sub, pub)
          case "requestBuilder" => new RequestBuilder(sub, pub, getMilliseconds("timeout"))
          case "simpleProcessBuilder" => new SimpleProcessBuilder(sub, pub, getStringList("components").toList, getMilliseconds("timeout"))
          case "averageAnalyser" => new AverageAnalyser(sub, pub, Categorization(getConfig("categorization")), Expression(getConfig("expression")), if(hasPath("window")){ Some(getConfig("window"))} else {None}) 
          case "alerter" => new Alerter(sub, pub, Categorization(getConfig("categorization")), Expression(getConfig("expression")))
          case "logProducer" => new LogProducer(sub, getString("loglevel"))
          case "camelProducer" => new CamelProducer(sub, getString("camelEndpoint"))
          case o => throw new ConfigurationException("processor type not recognized: %s".format(o))
        }
      } else if (hasPath("class")) {
        val clazz = getString("class")
        
        val arguments = if(hasPath("arguments")){
          getList("arguments").map { a => (a.unwrapped.getClass, a.unwrapped.asInstanceOf[AnyRef]) }
        } else {
          Nil
        }
        
        val pubs: Seq[(Class[_], AnyRef)] = if(hasPath("publication")) {
          Seq((classOf[Publication], pub))
        } else {
          Nil
        }
  
        val args = Seq((classOf[List[Subscription]], sub)) ++ pubs ++ arguments
        
        dynamicAccess.createInstanceFor[Processor](clazz, args).fold(throw _, p => p)
      } else {
        throw new ConfigurationException("processor must specify either a type or a class")
      }
    } catch {
      case e: ConfigException => throw new ConfigurationException(e.getMessage)
    }
  }
}
