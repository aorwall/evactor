package se.aorwall.logserver.microkernel

import se.aorwall.logserver.configuration.{ConfigurationServiceImpl, ConfigurationService}
import akka.actor.Actor._
import se.aorwall.logserver.receive.LogdataReceiver
import akka.config.Supervision._
import akka.actor.{Supervisor, TypedActor, SupervisorFactory}
import akka.config.TypedActorConfigurator
import com.google.inject.{Scopes, AbstractModule}
import se.aorwall.logserver.storage.{LogStorage, ConfigurationStorage}

/**
 * Used to boot up in Akka Microkernel
 *
 * TODO: Read from config file
 *
 */
class Boot {

 val manager = new TypedActorConfigurator

 manager
   .addExternalGuiceModule(new AbstractModule() {
      def configure {
        //TODO
      }})
   .inject
   .configure(
    OneForOneStrategy(List(classOf[Exception]), 3, 1000),
      Array(
        new SuperviseTypedActor(
          classOf[ConfigurationService],
          classOf[ConfigurationServiceImpl],
          Permanent,
          1000)
    ))
   .supervise

 val confService = manager.getInstance(classOf[ConfigurationService])

 val factory = Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Throwable]), 5, 5000),
      Supervise(
        actorOf(new LogdataReceiver("seda:logreceiver")).start,
        Permanent) ::
      Nil))

 factory.start


}