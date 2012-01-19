package se.aorwall.bam.irc

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IrcMonitorSuite  extends FunSuite with MustMatchers with BeforeAndAfterAll with Logging {

  test("poll irc") {
    
    val irc = new IrcMonitorKernel
    irc.startup()
    
    Thread.sleep(50000)
    
    irc.shutdown()
    
  }
  
}