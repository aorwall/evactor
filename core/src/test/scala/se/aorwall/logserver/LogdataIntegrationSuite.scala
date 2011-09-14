package se.aorwall.logserver

import monitor.statement.LatencyAnalyser
import monitor.statement.window.LengthWindow
import model.process.simple.{SimpleProcess, Component}
import model.{State, Log}
import monitor.{ActivityAnalyserPool, ActivityAnalyser}
import org.scalatest.FunSuite
import akka.util.duration._
import akka.actor.{ Actor}
import Actor._
import process.ProcessActor
import receive.LogdataReceiver
import akka.testkit.{CallingThreadDispatcher, TestKit}

/**
 * Testing the whole log data flow.
 */
class LogdataIntegrationSuite extends FunSuite with TestKit {

  test("Recieve a logdata objects and create activity object") {
    val processId = "process"

    val logReceiver = actorOf(new LogdataReceiver)
    val analyserPool = actorOf(new ActivityAnalyserPool)

    //val logStorage = TypedActor.newInstance(classOf[LogStorage], classOf[MockStorage], 1000)

    // Define the business process
    val startComp = new Component("startComponent", 1)
    val endComp = new Component("endComponent", 1)
    val process = new SimpleProcess(processId, List(startComp, endComp))

    // Define actors for processing and analysing activites
    val processActor = actorOf(new ProcessActor(process, analyserPool))
    val latencyActor = actorOf(new LatencyAnalyser(processId, testActor, 2000) with LengthWindow {override val noOfRequests = 2} )

    // Set CallingThreadDispatcher.global
    logReceiver.dispatcher = CallingThreadDispatcher.global
    processActor.dispatcher = CallingThreadDispatcher.global
    latencyActor.dispatcher = CallingThreadDispatcher.global

    // Start actors
    logReceiver.start()
    analyserPool.start()
    processActor.start()
    latencyActor.start()

    // Send some log objects

    within (1000 millis) {
      val currentTime = System.currentTimeMillis
      logReceiver ! new Log("server", "startComponent", "329380921309", "client", currentTime, State.START, "hello")
      logReceiver ! new Log("server", "startComponent", "329380921309", "client", currentTime+1000, State.SUCCESS, "") // success
      logReceiver ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+2000, State.START, "")
      logReceiver ! new Log("server", "endComponent", "329380921309", "startComponent", currentTime+3000, State.SUCCESS, "") // success
      expectMsg("Average latency 3000ms is higher than the maximum allowed latency 2000ms") // latency alert!
    }

    // Stop actors
    logReceiver.stop()
    processActor.stop()
    latencyActor.stop()
    analyserPool.stop()

    //TypedActor.stop(logStorage)
  }
}