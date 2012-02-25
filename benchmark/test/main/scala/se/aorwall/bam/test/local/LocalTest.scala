package se.aorwall.bam.test.local

import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import scala.util.Random
import java.util.UUID
import akka.actor.ActorSystem
import akka.actor.Props
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.process.ProcessorHandler
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.process.ProcessorEventBusExtension
import se.aorwall.bam.test.TestKernel
import se.aorwall.bam.test.RequestGenerator
import akka.testkit.TestProbe
import akka.testkit.TestKit
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.util.duration._
import akka.actor.ActorRef
import se.aorwall.bam.model.events.RequestEvent

@RunWith(classOf[JUnitRunner])
class LocalTest(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  import TestKernel._
  
  def this() = this(ActorSystem("LocalTest"))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  test("Load test") {   
	  
    val noOfProcesses: Int = businessProcesses.size
    val noOfRequestsPerProcess: Int = 10
    val threads = 10
    
    // initiate local environment
    val collector = system.actorOf(Props[Collector], name = "collect")
    val processor = system.actorOf(Props[ProcessorHandler], name = "process")
    processor ! new Request("requestProcessor", requestTimeout)
	  businessProcesses.foreach { processor ! _ }
	  
	  // run test
	  val countProbe = TestProbe()
	  
    val processCountProbe = TestProbe()    
	  val processClassifier = classOf[SimpleProcessEvent].getSimpleName + "/*"
	  ProcessorEventBusExtension(system).subscribe(processCountProbe.ref, processClassifier)
	  
	  val requestGenerators = List.fill(threads)(system.actorOf(Props(new RequestGenerator(businessProcesses, collector, countProbe.ref, noOfRequestsPerProcess))))
        	  
	  Thread.sleep(100)	  
	  
    val start = System.currentTimeMillis
    
    for(requestGen <- requestGenerators) {
      requestGen ! None  
    }
    
	  // verify that all processes were created
	  val count = noOfProcesses * noOfRequestsPerProcess * threads
	  
	  countProbe.receiveN(count, 2 minutes)    
	  println("Finished sending log events in " + (System.currentTimeMillis - start) + "ms")
  
	  processCountProbe.receiveN(count, 2 minutes)
    println("Finished processing "+ count +" process events in " + (System.currentTimeMillis - start) + "ms")
  }
}