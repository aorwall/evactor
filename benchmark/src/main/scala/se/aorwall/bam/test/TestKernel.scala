package se.aorwall.bam.test

import com.twitter.ostrich.admin.config.AdminServiceConfig
import com.twitter.ostrich.admin.config.ServerConfig
import com.twitter.ostrich.admin.RuntimeEnvironment

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.kernel.Bootable
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import se.aorwall.bam.process.extract.keyword.Keyword
import se.aorwall.bam.process.extract.kpi.Kpi
import se.aorwall.bam.process.ProcessorHandler

object TestKernel {  

  val businessProcesses = 
    new SimpleProcess("process1", List("component1_1", "component1_2"), 2000) :: 
    new SimpleProcess("process2", List("component2_1", "component2_2", "component2_3", "component2_4", "component2_5"), 20000) :: 
    new SimpleProcess("process3", List("component3_1", "component3_2", "component3_3"), 1000) :: 
    new SimpleProcess("process4", List("component4_1"), 3000) :: 
    new SimpleProcess("process5", List("component5_1", "component5_2", "component5_3", "component5_4"), 10000) :: 
    new SimpleProcess("process6", List("component6_1", "component6_2", "component6_3"), 1000) :: 
    new SimpleProcess("process7", List("component7_1", "component7_2", "component7_3", "component7_4"), 1000) :: 
    new SimpleProcess("process8", List("component8_1", "component8_2"), 2000) :: 
    new SimpleProcess("process9", List("component9_1", "component9_2", "component9_3"), 2000) :: Nil
    
}

class TestKernel extends Bootable {
  import TestKernel._
  
  lazy val system = ActorSystem("test")

  lazy val collector = system.actorOf(Props[Collector], name = "collect")
  lazy val processor = system.actorOf(Props[ProcessorHandler], name = "process")
	
	
  def startup = {          
    processor ! new Request("requestProcessor", 200L)
    businessProcesses.foreach { processor ! _ }
        
    // Start Ostrich admin web service
    val adminConfig = new AdminServiceConfig {
      httpPort = 8080
    }
	   
    val runtime = RuntimeEnvironment(this, Array[String]())
	 val admin = adminConfig()(runtime)		
  }

  def shutdown = {
    system.shutdown()
  }

}



