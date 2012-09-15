package org.evactor.expression

import org.junit.runner.RunWith
import org.evactor.EvactorSpec
import org.scalatest.junit.JUnitRunner
import org.evactor.model.events.Event

@RunWith(classOf[JUnitRunner])
class ExpressionSpec extends EvactorSpec {
  
  val mockEvent = new Event { val id ="foo"; val timestamp = 0L}
  
  "An Expression" must {
    
    "be able to be created from a Config object" in {
      val exprConfig = parseConfig("static = \"foo\"")
      val expr = Expression(exprConfig)
      
      expr.evaluate(mockEvent) must be (Some("foo"))
    }
    
    "be able to be a mvel expression from Config object" in {
      val exprConfig = parseConfig("mvel = \"id\"")
      val expr = Expression(exprConfig)
      
      expr.evaluate(mockEvent) must be (Some("foo"))
    }
    
  }
}