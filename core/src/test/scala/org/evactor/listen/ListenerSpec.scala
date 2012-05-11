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
package org.evactor.listen

import org.evactor.EvactorSpec
import com.typesafe.config.ConfigFactory
import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.actor.ActorRef

class TestListener(val actor: ActorRef, val foo: String) extends Listener {
  
  def receive = {
    case msg => actor ! msg
  }
  
}

@RunWith(classOf[JUnitRunner])
class ListenerSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec{

  def this() = this(ActorSystem("ListenerSpec"))

  "A Listener" must {
    
    "create a new custom listener" in {
      
      val listenerConfig = ConfigFactory.parseString("""
          class = org.evactor.listen.TestListener
          arguments = [ "bar" ]
        """)
      val listener = TestActorRef(Listener(listenerConfig, testActor))
      
      listener.underlyingActor match {
        case l: TestListener => l.foo must be ("bar")
        case _ => fail
      }
      
      listener ! "msg"
      expectMsg("msg")
    }
  }
}