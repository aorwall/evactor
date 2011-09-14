package se.aorwall.logserver.test

import akka.actor.Actor._
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.model.process.simple.{SimpleProcess, Component}
import util.Random
import akka.testkit.CallingThreadDispatcher
import se.aorwall.logserver.receive.{LogdataReceiverPool, LogdataReceiver}
import org.slf4j.LoggerFactory
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.LoggerContext
import akka.routing.CyclicIterator

object Main {

  val noOfComponents = 20
  val noOfProcesses = 10
  val components = 0 until noOfComponents map (x => new Component("component" + x, 1))
  val businessProcesses = 0 until noOfProcesses map (x => createBusinessProcess(x))

  def main(args: Array[String]) {

    System.setProperty("akka.config", "/home/albert/IdeaProjects/LogserverAkka/test/src/main/scala/akka.conf")

    LoggerFactory.getILoggerFactory() match {
      case lc: LoggerContext => StatusPrinter.print(lc)
    }

    val responseActor = actorOf(new ResponseActor)

    val processActors = businessProcesses.map(p => actorOf(new ProcessActor(p, responseActor)))
    val logReceiver = actorOf(new LogdataReceiver)

    // Start log analyser
    logReceiver.start()
    processActors.foreach(p => p.start())
    responseActor.start()

    // Create request actors
    val requestActors = businessProcesses map { process => actorOf(new RequestActor(process.components.map(_.componentId), logReceiver)) } toList

    requestActors.foreach(a => a.start())

    //val loadBalancer = loadBalancerActor( new CyclicIterator( requestActors ) )

    for(x <- 0 until 10)
      requestActors(x) ! "hej"

    // Stop log analyser
    logReceiver.stop()
    processActors.foreach(p => p.stop())
    requestActors.foreach(a => a.stop())
    responseActor.stop()

  }

  def createBusinessProcess(no: Int) =
      new SimpleProcess("process" + no, 0 until (Random.nextInt(4)+1) map (x => components(Random.nextInt(noOfComponents)) ) toList)

}