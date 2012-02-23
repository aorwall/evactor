package se.aorwall.bam.test.local

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
	  	  
	  val collector = system.actorFor("akka://test@darkthrone:2552/user/collect")
	  
	  Thread.sleep(100)
	  
	  val requestGen = system.actorOf(Props(new RequestGenerator(TestKernel.businessProcesses, noOfRequestsPerProcess, collector)), name = "requestGen")   
	  
	  Thread.sleep(100)
	  
	  
  }
}