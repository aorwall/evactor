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
import se.aorwall.bam.test.TestKernel

@RunWith(classOf[JUnitRunner])
class LocalTest(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  
  def this() = this(ActorSystem("RemoteTest"))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  /**
   * This test case is just used to send load to a remote akka system. It doesn't verify anything.
   */
  test("Load test") {   
	  
	  val noOfRequestsPerProcess: Int = 10
	  	  
	  val collector = remote !!
	  
	  Thread.sleep(100)
	  
	  val requestGen = system.actorOf(Props(new RequestGenerator(TestKernel.businessProcesses, noOfRequestsPerProcess, collector)), name = "requestGen")   
	  
	  Thread.sleep(100)
	  
	  
  }
}