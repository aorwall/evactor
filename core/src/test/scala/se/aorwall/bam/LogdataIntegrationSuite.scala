package se.aorwall.bam

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.TypedActor
import akka.testkit.CallingThreadDispatcher
import akka.testkit.TestKit
import collect.Collector
import grizzled.slf4j.Logging
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.storage.EventStorageSpec
import se.aorwall.bam.process.analyse.latency.Latency
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import se.aorwall.bam.process.analyse.window.LengthWindowConf

/**
 * Testing the whole log data flow.
 *
 */
@RunWith(classOf[JUnitRunner])
class LogdataIntegrationSuite(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  
  def this() = this(ActorSystem("LogdataIntegrationSuite", EventStorageSpec.storageConf))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  test("Recieve a logdata objects and send an alert") {
    
    var result: AlertEvent = null
    val processId = "processId"
    val camelEndpoint = "hej"

    // Start up the modules
    val system = _system
    val collector = system.actorOf(Props[Collector].withDispatcher(CallingThreadDispatcher.Id), name = "collect")
    val processor = system.actorOf(Props[ProcessorHandler].withDispatcher(CallingThreadDispatcher.Id), name = "process")
      
    // start the processors
    processor ! new Request("requestProcessor", 120000L)
    processor ! new SimpleProcess(processId, List("startComponent", "endComponent"), 120000l)  
    processor ! new Latency("latency", Some(processId), 2000, Some(new LengthWindowConf(2)))

    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(400)

    collector ! new LogEvent("startComponent", "329380921309", currentTime, "329380921309", "client", "server", Start, "hello")
    collector ! new LogEvent("startComponent", "329380921309", currentTime+1000, "329380921309", "client", "server" , Success, "") // success
    collector ! new LogEvent("endComponent", "329380921309", currentTime+2000, "329380921309", "client", "server", Start, "")
    collector ! new LogEvent("endComponent", "329380921309",  currentTime+3000, "329380921309", "client", "server", Success, "") // success

    Thread.sleep(400)
    
  	//expectMsg(1 seconds, new Alert(processId, "Average latency 3000ms is higher than the maximum allowed latency 2000ms", true)) // the latency alert

    TypedActor(system).stop(processor)
    //TypedActor(system).stop(analyser)
  }
}