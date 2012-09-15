package org.evactor.publish

import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.evactor.model.events.Event

@RunWith(classOf[JUnitRunner])
class PublicationSpec extends EvactorSpec {

  val mockEvent = new Event{ val id = ""; val timestamp = 0L}
  
  "A Publication" must {
    
    "be able to be created with static values" in {
      val pubConfig = parseConfig("{ channel = \"chan\" }")
      val publication = Publication(pubConfig)
      
      publication.channel(mockEvent) must be ("chan")
      
    }
    
    "be able to be created with expressions" in {
      val pubConfig = parseConfig("{ channel = { static = \"foo\"} }")
      val publication = Publication(pubConfig)
      
      publication.channel(mockEvent) must be ("foo")
    }
  }
}