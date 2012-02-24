package se.aorwall.bam.test.remote

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import grizzled.slf4j.Logging
import se.aorwall.bam.test.RequestGenerator
import se.aorwall.bam.test.TestKernel
import org.scalatest.junit.JUnitRunner
import se.aorwall.bam.model.events.LogEvent
import se.aorwall.bam.model.Start
import akka.testkit.TestProbe
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class RemoteTest(_system: ActorSystem) extends TestKit(_system) with FunSuite with MustMatchers with BeforeAndAfterAll with Logging {
  import TestKernel._
  
  def this() = this(ActorSystem("RemoteTest"))

  override protected def afterAll(): scala.Unit = {
    system.shutdown()
  }

  /**
   * This test case is just used to send load to a remote akka system. It doesn't verify anything.
   */
  test("Load test") {   
	  
	  val noOfRequestsPerProcess: Int = 1000
	  	  
	  val probe = TestProbe()
	  
	  val collector = system.actorFor("akka://test@darkthrone:2552/user/collect")
	  val timer = system.actorFor("akka://test@darkthrone:2552/user/timer")
	  val requestGen = system.actorOf(Props(new RequestGenerator(businessProcesses, noOfRequestsPerProcess, collector, probe.ref)), name = "requestGen")
//	  val requestGen2 = system.actorOf(Props(new RequestGenerator(businessProcesses, noOfRequestsPerProcess, collector, probe.ref)), name = "requestGen2")
	  
	  Thread.sleep(300)
  
	  val start = System.currentTimeMillis
	  
	  val count: Int = businessProcesses.size * noOfRequestsPerProcess * 2
	  
	  timer ! count
	  requestGen ! None
	  //requestGen2 ! None
	  
	  probe.receiveN(count, 1 minute)
	  
	  println("Finished in " + (System.currentTimeMillis - start))
	  
	  Thread.sleep(1000)
	  
  }
}