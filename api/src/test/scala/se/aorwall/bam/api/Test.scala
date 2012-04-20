package se.aorwall.bam.api

import akka.actor.ActorSystem

object Test {

  lazy val system = ActorSystem("api")

  def main(args: Array[String]): Unit = {
    
	 unfiltered.netty.Http(8080).plan(new BasePlan(system)).run()	 
	 
  }
  
}
