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
package org.evactor.transform

import org.evactor.EvactorSpec
import com.typesafe.config.ConfigFactory
import org.evactor.listen.Listener
import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.actor.ActorRef

class TestTransformer(val actor: ActorRef, val foo: String) extends Transformer {
  
  def receive = {
    case msg => actor ! msg
  }
  
}

@RunWith(classOf[JUnitRunner])
class TransformerSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with EvactorSpec{
  
  def this() = this(ActorSystem("TransformerSpec"))


  "A Transformer" must {
    
    "create a new custom transformer" in {
      
      val transformerConfig = ConfigFactory.parseString("""
          class = org.evactor.transform.TestTransformer
          arguments = [ "bar" ]
        """)
      val transformer = TestActorRef(Transformer(transformerConfig, testActor))
      
      transformer.underlyingActor match {
        case l: TestTransformer => l.foo must be ("bar") 
        case _ => fail
      }
      
      transformer ! "msg"
      expectMsg("msg")
    }
    
  }
  
}