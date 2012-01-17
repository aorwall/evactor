package se.aorwall.bam.storage

import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.LogEvent
import akka.actor.ActorSystemImpl
import akka.actor.Extension
import scala.util.DynamicVariable
import akka.actor.ActorSystem
import com.typesafe.config.Config
import akka.config.ConfigurationException
import akka.util.ReflectiveAccess
import grizzled.slf4j.Logging

/**
 * Handling event storage. A lot of inspiration from the serialization and durable mailbox
 * implementations in Akka.
 *  
 */
object EventStorageFactory {
  
  class Settings(val config: Config) {

    import scala.collection.JavaConverters._
    import config._

    val StorageImplementations: Map[String, String] =
      getConfig("akka.bam.storage.implementations").root.unwrapped.asScala.toMap.map { case (k, v) ⇒ (k, v.toString) }

    val StorageBindings: Map[String, Seq[String]] = {
      val configPath = "akka.bam.storage.storage-bindings"
      hasPath(configPath) match {
        case false ⇒ Map()
        case true ⇒
          val serializationBindings: Map[String, Seq[String]] = getConfig(configPath).root.unwrapped.asScala.toMap.map {
            case (k: String, v: java.util.Collection[_]) ⇒ (k -> v.asScala.toSeq.asInstanceOf[Seq[String]])
            case invalid ⇒ throw new ConfigurationException("Invalid storage-bindings [%s]".format(invalid))
          }
          serializationBindings

      }
    }
  }
}

class EventStorageFactory(val system: ActorSystemImpl) extends Extension with Logging {
  
   import EventStorageFactory._
  
	val settings = new Settings(system.settings.config)	
	
	def getEventStorage(event: Event): Option[EventStorage] = {
     info(event.getClass().getName)
     info(storageImplMap)
     val hej = storageImplMap.get(event.getClass().getName)
     info(hej)
     hej.getOrElse(None)
   }  

   def storageImplOf(storageFQN: String): Option[EventStorage] =
     storageFQN match {
      case "" => None
      case fqcn: String =>
        val constructorSignature = Array[Class[_]](classOf[ActorSystem])
        ReflectiveAccess.createInstance[EventStorage](fqcn, constructorSignature, Array[AnyRef](system)) match {
          case Right(instance) ⇒ Some(instance)
          case Left(exception) ⇒
            throw new IllegalArgumentException(
              ("Cannot instantiate EventStorage [%s], defined in [%s], " +
                "make sure it has constructor with a [akka.actor.ActorSystem] parameter")
                .format(fqcn, system), exception)
        }
    }

   lazy val storageImplementations: Map[String, Option[EventStorage]] = {
    val serializersConf = settings.StorageImplementations
    for ((k: String, v: String) <- serializersConf)
      yield k -> storageImplOf(v)
   }
	
   lazy val bindings: Map[String, String] = {
    settings.StorageBindings.foldLeft(Map[String, String]()) {
      //All keys which are lists, take the Strings from them and Map them
      case (result, (k: String, vs: Seq[_])) ⇒ result ++ (vs collect { case v: String ⇒ (v, k) })
      //For any other values, just skip them
      case (result, _) ⇒ result
    }
  }
   
  lazy val storageImplMap: Map[String, Option[EventStorage]] = bindings mapValues storageImplementations
   
}
