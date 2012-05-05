package org.evactor.listen

import akka.actor.Actor
import org.evactor.transform.Transformer
import akka.actor.ActorRef

abstract class ListenerConfiguration {

  def listener (sendTo: ActorRef): Listener
  
}

