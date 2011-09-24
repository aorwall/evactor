package se.aorwall.logserver.test

import akka.routing.Routing._
import se.aorwall.logserver.model.process.simple.{SimpleProcess, Component}
import util.Random
import se.aorwall.logserver.receive.{LogdataReceiverPool}
import akka.routing.CyclicIterator
import grizzled.slf4j.Logging
import java.util.UUID
import se.aorwall.logserver.process.{ActivityActor, ProcessActor}
import org.slf4j.LoggerFactory
import akka.actor.Actor
import Actor._
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.connection.SpeedForJOpTimer
import org.mockito.Mockito._
import se.aorwall.logserver.storage.{LogStorage, CassandraStorage}

object Main extends Logging {
  LoggerFactory.getILoggerFactory  // TODO: Temporary fix to get rid of http://www.slf4j.org/codes.html#substituteLogger

  val noOfProcesses = 10
  val businessProcesses = 0 until noOfProcesses map (x => createBusinessProcess(x))
  val noOfRequests = 100000

  def createBusinessProcess(no: Int) =
      new SimpleProcess("process" + no, 0 until (Random.nextInt(3)+1) map (x => new Component(UUID.randomUUID().toString+x, 0))  toList) // create process with a component list with unique id:s

  var finishedActivites = 0
  var lastFinishedActivity = 0L;

  def main(args: Array[String]) {

    // Need a cassandra server up and running on localhost 9160 with the correct Column Families
    val cassandraHostConfigurator = new CassandraHostConfigurator("localhost:9160")
    cassandraHostConfigurator.setOpTimer(new SpeedForJOpTimer("TestCluster"));
    val cluster = HFactory.getOrCreateCluster("TestCluster", cassandraHostConfigurator)

    val keyspace = HFactory.createKeyspace("LogserverTest", cluster)
    val storage = new CassandraStorage(keyspace)

    val responseActor = actorOf[ResponseActor]

    val processActors = businessProcesses.map(p => actorOf(new ProcessActor(p, storage, responseActor)))
    businessProcesses.foreach(p => info(p))

    val logReceiver = actorOf(new LogdataReceiverPool)

    // Start log analyser
    logReceiver.start()
    processActors.foreach(p => p.start())
    responseActor.start()

    // Create request actors
    val requestActorList = businessProcesses map { process => actorOf(new RequestActor(process.components.map(_.componentId), logReceiver)) } toList
    val requestActors = List.fill(10)(requestActorList).flatten

    requestActors.foreach(a => a.start())

    val loadBalancer = loadBalancerActor( new CyclicIterator( requestActors ) )

    val start = System.currentTimeMillis

    for(x <- 0 until noOfRequests)
      loadBalancer ! "hej"

    info("Finished requests after " + (System.currentTimeMillis-start) + " ms")

    var time = 0
    val maxTime = 180000
    while (finishedActivites < noOfRequests && time < maxTime){
        Thread.sleep(1000)
        time = time + 1000
    }

    info("No of finished activites: " + finishedActivites + ", last activity finished after " + (lastFinishedActivity - start))

    // Stop log analyser
    logReceiver.stop()
    processActors.foreach(a => a.stop())
    requestActors.foreach(a => a.stop())
    //loadBalancer ! Broadcast(PoisonPill)
    responseActor.stop()
    loadBalancer.stop()

    for (a <- Actor.registry.actorsFor[ActivityActor] ){
      info("Still active activity actor: " + a)
      a.stop
    }

    cluster.getConnectionManager().shutdown();
  }

  def count() = {
    finishedActivites += 1  //TODO: not thread safe...
    lastFinishedActivity = System.currentTimeMillis
  }


}