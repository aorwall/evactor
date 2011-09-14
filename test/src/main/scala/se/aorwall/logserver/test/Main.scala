package se.aorwall.logserver.test

import akka.actor.Actor._
import akka.actor.Actor
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.model.process.simple.{SimpleProcess, Component}
import util.Random
import akka.testkit.CallingThreadDispatcher
import se.aorwall.logserver.receive.{LogdataReceiverPool, LogdataReceiver}
import akka.routing.CyclicIterator
import grizzled.slf4j.Logging

object Main extends Logging {

  val noOfComponents = 20
  val noOfProcesses = 10
  val components = 0 until noOfComponents map (x => new Component("component" + x, 1))
  val businessProcesses = 0 until noOfProcesses map (x => createBusinessProcess(x))

  var finishedActivites = 0

  def main(args: Array[String]) {

    val responseActor = actorOf[ResponseActor]

    val processActors = businessProcesses.map(p => actorOf(new ProcessActor(p, responseActor)))
    val logReceiver = actorOf(new LogdataReceiverPool)

    // Start log analyser
    logReceiver.start()
    processActors.foreach(p => p.start())
    responseActor.start()

    // Create request actors
    val requestActors = businessProcesses map { process => actorOf(new RequestActor(process.components.map(_.componentId), logReceiver)) } toList

    requestActors.foreach(a => a.start())

    //val loadBalancer = loadBalancerActor( new CyclicIterator( requestActors ) )

    for(x <- 0 until 9)
      requestActors(x) ! "hej"

    Thread.sleep(5000)

    info("No of finished activites: " + finishedActivites)

    // Stop log analyser
    logReceiver.stop()
    processActors.foreach(p => p.stop())
    requestActors.foreach(a => a.stop())
    responseActor.stop()
  }

  def createBusinessProcess(no: Int) =
      new SimpleProcess("process" + no, 0 until (Random.nextInt(4)+1) map (x => components(Random.nextInt(noOfComponents)) ) toList)


  def count() = finishedActivites += 1  //TODO: not thread safe...

}