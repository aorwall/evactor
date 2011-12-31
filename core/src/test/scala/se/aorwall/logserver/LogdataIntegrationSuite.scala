package se.aorwall.logserver

import analyse.{AnalyserHandlerImpl, AnalyserHandler}
import configuration.{ConfigurationServiceImpl, ConfigurationService}
import model.statement.Latency
import model.statement.window.LengthWindowConf
import model.{Alert, State, Log}
import model.process.simple.{SimpleProcess, Component}
import org.mockito.Mockito._
import collect.Collector
import grizzled.slf4j.Logging
import org.scalatest.matchers.MustMatchers
import akka.actor.{Props, TypedActor, Actor, ActorSystem}
import process.{ProcessorHandlerImpl, ProcessorHandler}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import akka.testkit.{CallingThreadDispatcher, TestKit}

/**
 * Testing the whole log data flow.
 *
 * FIXME: This test doesn't shut down properly
 */
class LogdataIntegrationSuite(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {

  def this() = this(ActorSystem("LogdataIntegrationSuite"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  test("Recieve a logdata objects and send an alert") {

    var result: Alert = null
    val processId = "process"
    val camelEndpoint = "hej"

    // Start up the modules
    val system = ActorSystem("LogServerTest")
    val collector = system.actorOf(Props[Collector].withDispatcher(CallingThreadDispatcher.Id), name = "collect")
    val processor = TypedActor(system).typedActorOf(classOf[ProcessorHandler], new ProcessorHandlerImpl, Props().withDispatcher(CallingThreadDispatcher.Id), "process")
    val analyser = TypedActor(system).typedActorOf(classOf[AnalyserHandler], new AnalyserHandlerImpl, Props().withDispatcher(CallingThreadDispatcher.Id), "analyse")

    // Define and set up the business process
    val startComp = new Component("startComponent", 1)
    val endComp = new Component("endComponent", 1)
    val process = new SimpleProcess(processId, List(startComp, endComp), 1L)
    val latencyStmt = new Latency(processId, "statementId", camelEndpoint, 2000, Some(new LengthWindowConf(2)))

    processor.setProcess(process)
    analyser.addStatementToProcess(latencyStmt)

    // Collect logs
    val currentTime = System.currentTimeMillis

    Thread.sleep(200)

    debug("send logs to collector")
    collector ! new Log("server", "startComponent", "329380921309", "client", currentTime, State.START, "hello")
    collector ! new Log("server", "startComponent", "329380921309", "client", currentTime+1000, State.SUCCESS, "") // success
    collector ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+2000, State.START, "")
    collector ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+3000, State.SUCCESS, "") // success

    Thread.sleep(200)

    //result must be === new Alert(processId, "Average latency 3000ms is higher than the maximum allowed latency 2000ms", true) // the latency alert

    TypedActor(system).stop(processor)
    TypedActor(system).stop(analyser)
  }
}