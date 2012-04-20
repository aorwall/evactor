package se.aorwall.bam

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.CallingThreadDispatcher
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration.intToDurationInt
import collect.Collector
import se.aorwall.bam.model.events.AlertEvent
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Start
import se.aorwall.bam.model.Success
import se.aorwall.bam.process.analyse.latency.Latency
import se.aorwall.bam.process.analyse.window.LengthWindowConf
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import se.aorwall.bam.process.ProcessorEventBusExtension
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.Subscription
import se.aorwall.bam.storage.EventStorageSpec

/**
 * Testing the whole log data flow.
 *
 */
@RunWith(classOf[JUnitRunner])
class LogdataIntegrationSuite(_system: ActorSystem) 
	extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll {
  
  def this() = this(ActorSystem("LogdataIntegrationSuite", EventStorageSpec.storageConf))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Recieve log events and send an alert") {    
    
  	 val probe = TestProbe()
  	 
    var result: AlertEvent = null
    val processId = "processId"
    val camelEndpoint = "hej"

    // Start up the modules
    val collector = system.actorOf(Props[Collector].withDispatcher(CallingThreadDispatcher.Id), name = "collect")
    val processor = system.actorOf(Props[ProcessorHandler].withDispatcher(CallingThreadDispatcher.Id), name = "process")
          
    // start the processors
    val reqSubscriptions = List(new Subscription(Some("RequestEvent"), Some("startComponent"), None), new Subscription(Some("RequestEvent"), Some("endComponent"), None))  

    processor ! new Request("startComponent", List(new Subscription(Some("LogEvent"), Some("startComponent"), None)), 120000L)
  	processor ! new Request("endComponent", List(new Subscription(Some("LogEvent"), Some("endComponent"), None)), 120000L)
    processor ! new SimpleProcess("simpleProcess", reqSubscriptions, processId, None, 120000l)  
    processor ! new Latency("latency", List(new Subscription(Some("SimpleProcessEvent"), Some(processId), None)), "latency", None, 2000, Some(new LengthWindowConf(2)))

  	 ProcessorEventBusExtension(system).subscribe(probe.ref, new Subscription(Some("AlertEvent"), Some("latency"), None))
        
    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(400)

    collector ! new LogEvent("startComponent", None, "329380921309", currentTime, "329380921309", "client", "server", Start, "hello")
    collector ! new LogEvent("startComponent", None, "329380921309", currentTime+1000, "329380921309", "client", "server" , Success, "") // success
    collector ! new LogEvent("endComponent", None, "329380921309", currentTime+2000, "329380921309", "client", "server", Start, "")
    collector ! new LogEvent("endComponent", None, "329380921309",  currentTime+3000, "329380921309", "client", "server", Success, "") // success

    Thread.sleep(400)
    
  	 probe.expectMsgAllClassOf(1 seconds, classOf[AlertEvent]) // the latency alert
  }
}