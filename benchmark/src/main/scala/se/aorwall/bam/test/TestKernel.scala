package se.aorwall.bam.test

//import com.twitter.ostrich.admin.config.AdminServiceConfig
//import com.twitter.ostrich.admin.config.ServerConfig
//import com.twitter.ostrich.admin.RuntimeEnvironment
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.kernel.Bootable
import grizzled.slf4j.Logging
import se.aorwall.bam.collect.Collector
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.SimpleProcessEvent
import se.aorwall.bam.process.build.request.Request
import se.aorwall.bam.process.build.simpleprocess.SimpleProcess
import se.aorwall.bam.process.extract.keyword.Keyword
import se.aorwall.bam.process.extract.kpi.Kpi
import se.aorwall.bam.process._
import se.aorwall.bam.process.ProcessorEventBusExtension
import se.aorwall.bam.process.Subscription

object TestKernel {  

  val channels = List("channel1", "channel2", "channel3", "channel4", "channel5")
    
  val requestTimeout = 2000L  
}

class TestKernel extends Bootable {
  import TestKernel._

  lazy val system = ActorSystem("test")

  def startup = {         
    val collector = system.actorOf(Props[Collector], name = "collect")
    val processorHandler = system.actorOf(Props[ProcessorHandler], name = "process") 
    val timer = system.actorOf(Props[TimerActor], name = "timer") 
    
    // set up request processors
    for(channel <- channels){
      processorHandler ! new Request(channel, List(new Subscription(Some("LogEvent"), Some(channel), None)), requestTimeout)
    }
        
    val classifier = new Subscription(Some("RequestEvent"), None, None)
    ProcessorEventBusExtension(system).subscribe(timer, classifier)
          	 
    // start ostrich admin web service
//    val adminConfig = new AdminServiceConfig {
//      httpPort = 8080
//    }
//    val runtime = RuntimeEnvironment(this, Array[String]())
//	 val admin = adminConfig()(runtime)		
  }

  def shutdown = {
    system.shutdown()
  }

}

class TimerActor extends Actor with ActorLogging {

  var startTime: Long = 0
  var i: Int = 0
  var count: Int = 0
  
  def receive = {
    case c: Int => {
      count = c
      i = 0
      startTime = System.currentTimeMillis
      log.info("Will count to " + count + " events")
    } 
    case e: Event => {
      i = i+1
      if(i == count) log.info("It took " + (System.currentTimeMillis - startTime) + "ms to process " + count + " events" ) 
    }
  }
}

