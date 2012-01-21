package se.aorwall.bam.api

import akka.actor.ActorSystem

object Test {

  lazy val system = ActorSystem("IrcMonitor")

  def main(args: Array[String]): Unit = {
    
	 unfiltered.netty.Http(8080).plan(new DataEventAPI(system)).plan(new KeywordEventAPI(system)).run()	 
	 
  } 
  
}