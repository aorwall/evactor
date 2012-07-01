package org.evactor.subscribe

import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class SubscriptionSpec extends EvactorSpec{
  
  "A Subscriptions object" must {
    
    "must be able to create a subscription to a channel" in {
      val subConfig = parseConfig("subscriptions = [ {channel = \"foo\"} ]").getConfigList("subscriptions").toList
      val subs = Subscriptions(subConfig)
      
      subs.size must be (1)
      subs.get(0).channel must be (Some("foo"))
      
    }
    
    "must be able to create a subscription to a channel and a category" in {
      val subConfig = parseConfig("subscriptions = [ {channel = \"foo\" } ]").getConfigList("subscriptions").toList
      val subs = Subscriptions(subConfig)
      
      subs.size must be (1)
      subs.get(0).channel must be (Some("foo"))
    }
    
    "must be able to create a subscription to everything" in {
      val subConfig = parseConfig("subscriptions = [ {} ]").getConfigList("subscriptions").toList
      val subs = Subscriptions(subConfig)
      
      subs.size must be (1)
      subs.get(0).channel must be (None)
    }
    
    "must be able to create a list of subscriptions" in {
      val subConfig = parseConfig("subscriptions = [ {channel = \"foo\"}, {channel = \"bar\"} ]").getConfigList("subscriptions").toList
      val subs = Subscriptions(subConfig)
      
      subs.size must be (2)
      subs.get(0).channel must be (Some("foo"))
      subs.get(1).channel must be (Some("bar"))
    }
    
  }
}