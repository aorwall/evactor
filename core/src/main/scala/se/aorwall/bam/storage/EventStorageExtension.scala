package se.aorwall.bam.storage
import akka.actor.{ ActorSystem, ExtensionId, ExtensionIdProvider, ActorSystemImpl }
import akka.actor.ExtendedActorSystem

object EventStorageExtension extends ExtensionId[EventStorageFactory] with ExtensionIdProvider {
  
  override def get(system: ActorSystem): EventStorageFactory = super.get(system)
  
  override def lookup = EventStorageExtension
  
  override def createExtension(system: ExtendedActorSystem): EventStorageFactory = 
    new EventStorageFactory(system)
  
}
