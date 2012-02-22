package se.aorwall.bam.process
import akka.testkit.TestKit
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import se.aorwall.bam.model.events.DataEvent
import akka.util.duration._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProcessorEventBusSpec (_system: ActorSystem) 
  extends TestKit(_system) 
  with WordSpec 
  with BeforeAndAfterAll 
  with MustMatchers 
  with BeforeAndAfter {

  def this() = this(ActorSystem("ProcessorEventBusSpec"))

  override protected def afterAll(): scala.Unit = {
     _system.shutdown()
  }
    
  val bus = ProcessorEventBusExtension(_system)
  
  val eventpath_one_eventname = classOf[DataEvent].getSimpleName + "/foo";
  val eventpath_all_eventnames = classOf[DataEvent].getSimpleName + "/*";
  
  val event1 = new DataEvent("foo", "", 0L, "")
  val event2 = new DataEvent("bar", "", 0L, "")
  
  "A ProcessorEventBus" must {
    
    "publish the given event to the subscriber subscribing to the exact event name" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, eventpath_one_eventname)
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, eventpath_one_eventname)
    }

    "publish the given event to the subscriber subscribing to all event names on that path" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, eventpath_all_eventnames)
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, eventpath_all_eventnames)
    }

    "publish an event with another event name to the subscriber subscribing to all event names but not the subscriber subscribing to another event name " in {      
      val subscriber1 = TestProbe()
      val subscriber2 = TestProbe()
      bus.subscribe(subscriber1.ref, eventpath_one_eventname)
      bus.subscribe(subscriber2.ref, eventpath_all_eventnames)
      bus.publish(event2)
      subscriber2.expectMsg(1 second, event2)
      subscriber1.expectNoMsg(1 second)
      subscriber2.expectNoMsg(1 second)
      bus.unsubscribe(subscriber1.ref, eventpath_one_eventname)
      bus.unsubscribe(subscriber2.ref, eventpath_all_eventnames)
    }	  
        
    "publish an event with another event name to the subscriber subscribing to all events" in {
      val subscriber = TestProbe()
      bus.subscribe(subscriber.ref, "")
      bus.publish(event1)
      subscriber.expectMsg(1 second, event1)
      subscriber.expectNoMsg(1 second)
      bus.unsubscribe(subscriber.ref, "")      
    }
  }
}