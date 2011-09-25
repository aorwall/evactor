package se.aorwall.logserver

import configuration.{ConfigurationServiceImpl, ConfigurationService}
import model.statement.Latency
import model.statement.window.LengthWindowConf
import model.{Alert, State, Log}
import model.process.simple.{SimpleProcess, Component}
import org.scalatest.FunSuite
import org.mockito.Mockito._
import receive.LogdataReceiver
import akka.testkit.{CallingThreadDispatcher, TestKit}
import akka.actor.{TypedActor, Actor}
import Actor._
import storage.{ConfigurationStorage, LogStorage}
import akka.event.EventHandler
import akka.camel.CamelServiceManager
import grizzled.slf4j.Logging
import akka.camel.{Message, Consumer, CamelContextManager}
import org.scalatest.matchers.MustMatchers
import akka.config.Supervision.OneForOneStrategy

/**
 * Testing the whole log data flow.
 *
 * FIXME: This test doesn't shut down properly
 */
class LogdataIntegrationSuite extends FunSuite with MustMatchers with TestKit with Logging {

  test("Recieve a logdata objects and send an alert") {

    val camelEndpoint = "direct:test"
    var result: Alert = null

    val alertReceiver = actorOf(new Actor with Consumer {
      self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

      def endpointUri = camelEndpoint

      def receive = {
        case msg: Message => info(msg.bodyAs[Alert])
      }
    })

    val service = CamelServiceManager.startCamelService

    val processId = "process"
    val logStorage = mock(classOf[LogStorage])
    val confStorage = mock(classOf[ConfigurationStorage])
    when(logStorage.readLogs("process:329380921309")).thenReturn(List())
    when(confStorage.readAllBusinessProcesses()).thenReturn(List())
    when(confStorage.readStatements(processId)).thenReturn(List())
    val logReceiver = actorOf(new LogdataReceiver("direct:logreceiver"))

    // Define the business process
    val startComp = new Component("startComponent", 1)
    val endComp = new Component("endComponent", 1)
    val process = new SimpleProcess(processId, List(startComp, endComp), 0L)
    val latencyStmt = new Latency(processId, "statementId", camelEndpoint, 2000, Some(new LengthWindowConf(2)))

    val confService = TypedActor.newInstance(classOf[ConfigurationService], new ConfigurationServiceImpl(confStorage, logStorage))
    confService.addBusinessProcess(process)
    confService.addStatementToProcess(latencyStmt)

    // Set CallingThreadDispatcher.global
    logReceiver.dispatcher = CallingThreadDispatcher.global

    // Start actors
    service.awaitEndpointActivation(2) {
      logReceiver.start()
      alertReceiver.start()
    } must be === true


    Thread.sleep(200L)

    val currentTime = System.currentTimeMillis

    logReceiver ! new Log("server", "startComponent", "329380921309", "client", currentTime, State.START, "hello")
    logReceiver ! new Log("server", "startComponent", "329380921309", "client", currentTime+1000, State.SUCCESS, "") // success
    logReceiver ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+2000, State.START, "")
    logReceiver ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+3000, State.SUCCESS, "") // success

    Thread.sleep(200L)

    // Stop actors
    TypedActor.stop(confService)

    service.awaitEndpointDeactivation(2) {
      alertReceiver.stop()
      logReceiver.stop()
    } must be === true
    service.stop

    Actor.registry.shutdownAll()
    EventHandler.shutdown()

    result must be === new Alert(processId, "Average latency 3000ms is higher than the maximum allowed latency 2000ms", true) // the latency alert
  }
}