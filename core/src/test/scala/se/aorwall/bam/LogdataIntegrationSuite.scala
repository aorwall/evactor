package se.aorwall.bam

import configuration.{ConfigurationServiceImpl, ConfigurationService}
import model.statement.Latency
import model.statement.window.LengthWindowConf
import org.mockito.Mockito._
import collect.Collector
import grizzled.slf4j.Logging
import org.scalatest.matchers.MustMatchers
import akka.actor.{Props, TypedActor, Actor, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import akka.testkit.{CallingThreadDispatcher, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.process.simple.SimpleProcessProcessor
import se.aorwall.bam.process.request.RequestProcessor
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Alert
import se.aorwall.bam.model.State
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.request.Request
import se.aorwall.bam.process.simple.SimpleProcess

/**
 * Testing the whole log data flow.
 *
 * FIXME: This test doesn't shut down properly
 */
@RunWith(classOf[JUnitRunner])
class LogdataIntegrationSuite(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {

  def this() = this(ActorSystem("LogdataIntegrationSuite"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  test("Recieve a logdata objects and send an alert") {
    
    var result: Alert = null
    val processId = "processId"
    val camelEndpoint = "hej"

    // Start up the modules
    val system = ActorSystem("LogServerTest")
    val collector = system.actorOf(Props[Collector].withDispatcher(CallingThreadDispatcher.Id), name = "collect")
    val processor = system.actorOf(Props[ProcessorHandler].withDispatcher(CallingThreadDispatcher.Id), name = "process")
//    val analyser = TypedActor(system).typedActorOf(classOf[AnalyserHandler], new AnalyserHandlerImpl, Props().withDispatcher(CallingThreadDispatcher.Id), "analyse")

    // start the processors
    processor ! new Request("requestProcessor", 120000L)
    processor ! new SimpleProcess(processId, List("startComponent", "endComponent"), 120000l)
            
    val latencyStmt = new Latency(processId, "statementId", camelEndpoint, 2000, Some(new LengthWindowConf(2)))

    //analyser.createProcessAnalyser(process)
    //analyser.addStatementToProcess(latencyStmt)

    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(400)

    collector ! new LogEvent("startComponent", "329380921309", currentTime, "329380921309", "client", "server", State.START, "hello")
    collector ! new LogEvent("startComponent", "329380921309", currentTime+1000, "329380921309", "client", "server" , State.SUCCESS, "") // success
    collector ! new LogEvent("endComponent", "329380921309", currentTime+2000, "329380921309", "client", "server", State.START, "")
    collector ! new LogEvent("endComponent", "329380921309",  currentTime+3000, "329380921309", "client", "server", State.SUCCESS, "") // success

    Thread.sleep(1000)

    //result must be === new Alert(processId, "Average latency 3000ms is higher than the maximum allowed latency 2000ms", true) // the latency alert

    TypedActor(system).stop(processor)
    //TypedActor(system).stop(analyser)
  }
}