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
 * Handling event storage. Made in the same way as the serialization
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
        case false => Map()
        case true => getConfig(configPath).root.unwrapped.asScala.toMap.map {
            case (k: String, v: java.util.Collection[_]) ⇒ (k -> v.asScala.toSeq.asInstanceOf[Seq[String]])
            case invalid ⇒ throw new ConfigurationException("Invalid storage-bindings [%s]".format(invalid))
          }
      }
    }
  }
}

class EventStorageFactory(val system: ActorSystemImpl) extends Extension with Logging {
  
   import EventStorageFactory._
  
	val settings = new Settings(system.settings.config)	
	
	def getEventStorage(event: Event): Option[EventStorage] = {
      storageImplMap.get(event.getClass().getName).getOrElse(None)
   }  

	def getEventStorage(className: String): Option[EventStorage] = {
     storageImplMap.get(className).getOrElse(None)
   }  
	
   def storageImplOf(storageFQN: String): Option[EventStorage] = {
     info(storageFQN)
   
     storageFQN match {
      case "" => None
      case fqcn: String =>
        val constructorSignature = Array[Class[_]](classOf[ActorSystem])
        ReflectiveAccess.createInstance[EventStorage](fqcn, constructorSignature, Array[AnyRef](system)) match {
          case Right(instance) ⇒ Some(instance)
          case Left(exception) ⇒
            throw new IllegalArgumentException(
              ("Cannot instantiate EventStorage [%s], defined in [%s]")
                .format(fqcn, system), exception)
        }
    }
   }
   
   lazy val storageImplementations: Map[String, Option[EventStorage]] = {     
    val storageConf = settings.StorageImplementations
    for ((k: String, v: String) <- storageConf)
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
