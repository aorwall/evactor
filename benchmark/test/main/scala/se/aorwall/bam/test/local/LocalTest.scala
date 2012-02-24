package se.aorwall.bam.test.local

import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import scala.util.Random
import java.util.UUID
import se.aorwall.bam.test.RequestGenerator
import akka.actor.ActorSystem
import akka.actor.Props
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.process.ProcessorEventBusExtension
import akka.testkit.TestProbe
import akka.testkit.TestKit
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.util.duration._
import se.aorwall.bam.test.TestKernel.businessProcesses

@RunWith(classOf[JUnitRunner])
class RemoteTest(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  
  def this() = this(ActorSystem("LocalTest"))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Load test") {   
	  
	  //val noOfProcesses: Int = 100
     val noOfProcesses: Int = businessProcesses.size
	  val noOfRequestsPerProcess: Int = 2
	  //val businessProcesses = 0 until noOfProcesses map (x => new SimpleProcess("process" + x, 0 until Random.nextInt(4)+1 map (x => UUID.randomUUID().toString) toList, 1000L))
	  	  
	  // initiate local environment
	  val collector = system.actorOf(Props[Collector], name = "collect")
	  val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	  processor ! new Request("requestProcessor", 200L)
	  businessProcesses.foreach { processor ! _ }
	  
	  // run test
	  val probe = TestProbe()
	  val classifier = classOf[SimpleProcessEvent].getSimpleName + "/*"
	  ProcessorEventBusExtension(system).subscribe(probe.ref, classifier)
	        	  
	  Thread.sleep(100)
	  
	  val requestGen = system.actorOf(Props(new RequestGenerator(businessProcesses, noOfRequestsPerProcess, collector)), name = "requestGen")   
	  
	  Thread.sleep(100)
	  
	  requestGen ! None
	  
	  // verify that all processes were created
	  probe.receiveN(noOfProcesses * noOfRequestsPerProcess, 10 seconds)
	  
	  Thread.sleep(3000)
	  
  }
}