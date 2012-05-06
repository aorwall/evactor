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
package org.evactor

import org.evactor.api.ApiServer
import org.evactor.api.BasePlan
import org.evactor.collect.Collector
import org.evactor.expression.MvelExpression
import org.evactor.expression.StaticExpression
import org.evactor.process.alert.log.LogAlerter
import org.evactor.process.route.Filter
import org.evactor.process.route.Forwarder
import org.evactor.subscribe.Subscription
import org.evactor.storage.StorageProcessorRouter
import org.evactor.twitter.listener.TwitterListener
import org.evactor.twitter.transformer.TwitterJsonToStatusEvent
import com.twitter.ostrich.admin.config._
import com.twitter.ostrich.admin.RuntimeEnvironment
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.kernel.Bootable
import org.evactor.collect.CollectorManager
import org.evactor.process.ProcessorManager
import org.evactor.storage.StorageManager
import org.evactor.twitter.listener.TwitterListenerConfig
import org.evactor.collect.AddCollector
import org.evactor.twitter.transformer.TwitterJsonToStatusEventConfig
import org.evactor.process.route.ForwarderConfig
import org.evactor.process.route.FilterConfig
import org.evactor.storage.StorageProcessorConfig
import org.evactor.publish.StaticPublication
import org.evactor.publish.DynamicPublication
import org.evactor.process.analyse.count.CountAnalyserConfig

object ExampleKernel {
  //test
  def main(args: Array[String]){	
    val foo = new ExampleKernel 
    foo.startup()  
  }
}

class ExampleKernel extends Bootable {
  
  lazy val system = ActorSystem("twitterExample")
  
  lazy val username = system.settings.config.getString("akka.evactor.example.twitter.username")
  lazy val password = system.settings.config.getString("akka.evactor.example.twitter.password")
  
  // managers
  lazy val pm = system.actorOf(Props[ProcessorManager], name = "process")
  lazy val cm = system.actorOf(Props[CollectorManager], name = "collect")
  lazy val sm = system.actorOf(Props[StorageManager], name = "store")
  
  // netty api server
  lazy val nettyServer = unfiltered.netty.Http(8080).plan(new BasePlan(system))
  
  def startup = {    
    
    // start twitter stream collector publishing status events on the channel "twitter"
    val listener = new TwitterListenerConfig("https://stream.twitter.com/1/statuses/sample.json", "%s:%s".format(username, password))
    cm ! new AddCollector("twitterCollector", Some(listener), Some(new TwitterJsonToStatusEventConfig()), new StaticPublication("twitter", Set()))
    
    val twitterSub = List(new Subscription("twitter"))
    
    // filter and categorize hashtags and publish to channel "twitter:keywords"
    val hashtagPublication = new DynamicPublication(new StaticExpression("twitter:hashtag"), Some(new MvelExpression("hashtags")))
    pm ! new FilterConfig("hashtagFilter", twitterSub, hashtagPublication, new MvelExpression("hashtags.size() > 0"), true)

    // filter and categorize urls and publish to channel "twitter:url"
    val urlPublication = new DynamicPublication(new StaticExpression("twitter:url"), Some(new MvelExpression("urls")))
    pm ! new FilterConfig("urlFilter", twitterSub, urlPublication, new MvelExpression("urls.size() > 0"), true)
    
    // count urls and send alerts about urls that occured more than 5 times in an hour to channel "twitter:url:popular"
    val urlSub = List(new Subscription("twitter:url"))
    val urlPub = new DynamicPublication(new StaticExpression("twitter:url:popular"), Some(new MvelExpression("message")))
    pm ! new CountAnalyserConfig("urlCounter", urlSub, urlPub, true, 5, 3600*1000)
    
    // count hashtags and send alerts about hashtags that occured more than 10 times in an hour to channel "twitter:hashtag:popular"
    val hashtagSub = List(new Subscription("twitter:hashtag"))
    val hashtagPub = new DynamicPublication(new StaticExpression("twitter:hashtag:popular"), Some(new MvelExpression("message")))
    pm ! new CountAnalyserConfig("hashtagCounter", hashtagSub, hashtagPub, true, 10, 3600*1000)
    
    // alert on scala, akka and cassandra
    //    val subs = List(new Subscription("twitter:keywords", "scala"), new Subscription("twitter:keywords", "akka"), new Subscription("twitter:keywords", "cassandra"), new Subscription("twitter:url:alert"))
    //    val logger = system.actorOf(Props(new LogAlerter(subs)), name = "logger")
    
    // store everything
    sm ! new StorageProcessorConfig(None, 10)
    
    // start api server
    nettyServer.start()
    
    // start ostrich admin web service
    val adminConfig = new AdminServiceConfig {
      httpPort = 8888
    }

    val runtime = RuntimeEnvironment(this, Array[String]())
    val admin = adminConfig()(runtime)
  }

  def shutdown = {
    nettyServer.stop()
    system.shutdown()
  }

}

