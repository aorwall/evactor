package se.aorwall.bam.process

import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.junit.runner.RunWith
import se.aorwall.bam.BamSpec
import org.scalatest.junit.JUnitRunner
import akka.testkit.TestActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import akka.dispatch.Await
import se.aorwall.bam.model.events.Event

@RunWith(classOf[JUnitRunner])
class ProcessorHandlerSpec(_system: ActorSystem) 
  extends TestKit(_system) 
  with BamSpec   
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("ProcessorHandlerSpec"))

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }
  
  val testConf = new ProcessorConfiguration("name", Nil){
    def processor = new Processor (Nil) {
	    type T = Event
	    def process(event: Event) {}
	  }
  }
  
  implicit val timeout = Timeout(1 second)
    
  "A Processor handler" must {
	
	  "must report if it successfully started a new processor" in {
	    val handler = TestActorRef[ProcessorHandler]
	    val future = handler ? testConf 
      future onFailure {
        case _ => fail
      }
	  }
     
    "must report a failure on attempts to add a processor with the same name twice" in {
      val handler = TestActorRef[ProcessorHandler]
      val future1 = handler ? testConf 
      future1 onFailure {
        case _ => fail
      }
      val future2 = handler ? testConf 
      future2 onSuccess {
        case _ => fail
      }
    }
      
    "must report if it successfully removed a processor" in {
      val handler = TestActorRef[ProcessorHandler]
      val future1 = handler ? testConf 
      future1 onFailure {
        case _ => fail
      }
      val future2 = handler ? "name" 
      future2 onFailure {
        case _ => fail
      }
    }
    
    "must report if it couldn't remove a processor" in {
      val handler = TestActorRef[ProcessorHandler]
      val future = handler ? "name" 
      future onSuccess {
        case _ => fail
      }
    }
  }
}