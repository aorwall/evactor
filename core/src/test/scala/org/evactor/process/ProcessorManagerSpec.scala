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

import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.evactor.EvactorSpec
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.util.duration._
import scala.concurrent.Await
import org.evactor.model.events.Event
import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class ProcessorManagerSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec   
  with BeforeAndAfterAll {

  import _system.dispatcher
  
  def this() = this(ActorSystem("ProcessorManagerSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  val testConf = """
    filter {
      name = "twitter_url_filter"
      subscriptions = [ {channel = "twitter"} ]
      publication = { channel = static {"twitter:url"}, categories = mvel {"urls"} }
      filterExpression = { mvel = "urls == null || urls.size() == 0" }
      accept = false
    }
  """  
  
  implicit val timeout = Timeout(1 second)
    
  "A Processor handler" must {
	
	  "must report if it successfully started a new processor" in {
	    val manager = TestActorRef[ProcessorManager]
	    val future = manager ? testConf 
      future onFailure {
        case _ => fail
      }
	  }
     
    "must report a failure on attempts to add a processor with the same name twice" in {
      val manager = TestActorRef[ProcessorManager]
      val future1 = manager ? testConf 
      future1 onFailure {
        case _ => fail
      }
      val future2 = manager ? testConf 
      future2 onSuccess {
        case _ => fail
      }
    }
      
    "must report if it successfully removed a processor" in {
      val manager = TestActorRef[ProcessorManager]
      val future1 = manager ? testConf 
      future1 onFailure {
        case _ => fail
      }
      val future2 = manager ? "name" 
      future2 onFailure {
        case _ => fail
      }
    }
    
    "must report if it couldn't remove a processor" in {
      val manager = TestActorRef[ProcessorManager]
      val future = manager ? "name" 
      future onSuccess {
        case _ => fail
      }
    }
  }
}