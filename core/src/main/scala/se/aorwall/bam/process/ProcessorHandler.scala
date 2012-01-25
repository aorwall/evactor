package se.aorwall.bam.process

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.routing.BroadcastRouter
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.Event
import akka.util.Index

class ProcessorHandler extends Actor with Logging  {
  
  /**
   * Will try to do a more refined solution for selecting the 
   * right processors for each event later...
   */
  val processorsWithEventName = new Index[String, ActorRef](100, _ compareTo _)
  
  override def preStart = {
    debug(context.self + " starting..")
  }
  
  def receive = {
    case event: Event => process(event)
    case configuration: ProcessorConfiguration => setProcessor(configuration)
    case processorId: String => removeProcessor(processorId)
    case msg => info(context.self + " can't handle: " + msg)
  }
  
  def process(event: Event) {
    trace(context.self + " received event: " + event)
    context.children.foreach(child => child ! event)
  }
    
  def setProcessor(configuration: ProcessorConfiguration) {
    debug(context.self + " setting processor for configuration: " + configuration)
    
    // stopping previous actor if one exists
    val runningActor = context.actorFor(configuration.name)
    context.stop(runningActor)          
    context.actorOf(Props(configuration.getProcessor), name = configuration.name)
  }

  def removeProcessor(processorId: String) {
    debug(context.self + " stopping processor for process: " + processorId)
    val runningActor = context.actorFor(processorId)
    context.stop(runningActor)    
  }

  override def postStop(): Unit = {
    debug(context.self + " stopping..")
  }
}
