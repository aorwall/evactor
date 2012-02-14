package se.aorwall.bam.irc

import grizzled.slf4j.Logging

object IrcMonitorTest extends Logging {
    
  def main(args: Array[String]): Unit = {
    
    val irc = new IrcMonitorKernel
    irc.startup()
    
    while(true){}
    
    irc.shutdown()
    
  }
  
}