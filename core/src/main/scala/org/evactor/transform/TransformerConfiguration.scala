package org.evactor.transform

import org.evactor.collect.Collector
import akka.actor.ActorRef
import akka.actor.Actor

abstract class TransformerConfiguration {

  def transformer(sendTo: ActorRef): Transformer
  
}