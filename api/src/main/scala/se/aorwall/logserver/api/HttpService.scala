package se.aorwall.logserver.api

import akka.actor.{ActorRef, Actor}
import Actor._
import akka.http.{RootEndpoint, Endpoint}
import se.aorwall.logserver.cassandra.CassandraUtil
import se.aorwall.logserver.storage.CassandraStorage

class HttpService extends Actor with Endpoint {
  final val ServiceRoot = "/logserver/"
  final val GetStatistics = ServiceRoot + "statistics"

  self.dispatcher = Endpoint.Dispatcher

  def hook(uri: String): Boolean = (uri == GetStatistics)

  def provide(uri: String): ActorRef = {
    statistics
  }

  override def preStart() = {
      val root = Actor.registry.actorsFor(classOf[RootEndpoint]).head
      root ! Endpoint.Attach(hook, provide)
  }

  def receive = handleHttpRequest

  // TODO: Provide an actor pool instead
  // TODO: Hard coded depdendency to test api against cassandra
  val cassandraStorage = new CassandraStorage(CassandraUtil.getKeyspace("TestCluster", "localhost", 9160, "LogserverTest"))

  lazy val statistics = actorOf(new GetStatistics(cassandraStorage)).start()

}
