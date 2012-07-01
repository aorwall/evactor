package org.evactor.process;

import org.evactor.EvactorSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.typesafe.config.ConfigFactory
import org.evactor.expression.StaticExpression

@RunWith(classOf[JUnitRunner])
class CategoryProcessorSpec extends EvactorSpec {

  "A Categorization object" must {
    
    "create a Categorization object based on a NoCategorization config" in {
      val conf = ConfigFactory.parseString("{ NoCategorization {} }")
      val cat = Categorization(conf)
      cat match {
        case NoCategorization() => 
        case _ => fail()
      }
    }
    
    "create a Categorization object based on a OneAndOne config" in {
      val conf = ConfigFactory.parseString("{ OneAndOne { static = \"foo\" } }") 
      val cat = Categorization(conf)
      cat match {
        case OneAndOne(expr) => expr match {
          case s: StaticExpression =>
          case _ => fail()
        }
        case _ => fail()
      }
    }

    "create a Categorization object based on a AllInOne config" in {
      val conf = ConfigFactory.parseString("{ AllInOne { static = \"foo\" } }") 
      val cat = Categorization(conf)
      cat match {
        case AllInOne(expr) => expr match {
          case s: StaticExpression =>
          case _ => fail()
        }
        case _ => fail()
      }
    }
  }
  
}
