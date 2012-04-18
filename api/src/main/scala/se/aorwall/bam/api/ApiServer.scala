package se.aorwall.bam.api

import akka.actor.Actor
import akka.actor.ActorSystem

class ApiServer(
    val system: ActorSystem,
    val port: Int)
  extends Actor  {

  lazy val nettyServer = unfiltered.netty.Http(port).plan(new BasePlan(system))

  def receive = {
    case "startup" => nettyServer.run
    case _      	 => nettyServer.stop
  }
  
}
