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
import com.twitter.ostrich.admin.config._
import com.twitter.ostrich.admin.RuntimeEnvironment
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.kernel.Bootable

object ExampleKernel {
  //test
  def main(args: Array[String]){
    val foo = new ExampleKernel
    foo.startup()
  }
}

class ExampleKernel extends Bootable {
  
  lazy val system = ActorSystem("twitterExample")
  
  // context
  lazy val context = system.actorOf(Props[EvactorContext], name = "evactor")
  
  // netty api server
  val port = if(system.settings.config.hasPath("evactor.api.port")){
    system.settings.config.getInt("evactor.api.port")
  } else {
    8080
  }

//  lazy val nettyServer = unfiltered.netty.Http(port).plan(new BasePlan(system))

  def startup = {
    
    if(!system.settings.config.hasPath("evactor")) throw new RuntimeException("No configuration found!")
    
    context  // Start evactor context
    
    
    if(system.settings.config.hasPath("evactor.api")){
      // start api server
//      nettyServer.start()
    }
    
    if(system.settings.config.hasPath("evactor.monitoring.ostrich")){
      // start ostrich admin web service
      val adminConfig = new AdminServiceConfig {
        httpPort = system.settings.config.getInt("evactor.monitoring.ostrich.port")
      }
      
      val runtime = RuntimeEnvironment(this, Array[String]())
      val admin = adminConfig()(runtime)
    }
  }

  def shutdown = {
    if(system.settings.config.hasPath("evactor.api")){
//      nettyServer.stop()
    }
    system.shutdown()
  }

}

