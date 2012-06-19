package org.evactor.broker

import com.typesafe.config.Config
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.actor.ReflectiveDynamicAccess
import akka.util.duration._
import org.evactor.ConfigurationException
import org.evactor.Start
import akka.camel.CamelExtension
import org.apache.activemq.camel.component.ActiveMQComponent
import scala.collection.JavaConversions._
import org.apache.camel.Component


/**
 * User: anders
 */

class BrokerManager extends Actor with ActorLogging {
  val config = context.system.settings.config
  lazy val camel = CamelExtension(context.system)
  lazy val dynamicAccess = new ReflectiveDynamicAccess(this.getClass.getClassLoader)


  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e: ConfigurationException => log.error("Stopping brokers because of: {}", e); Stop
    case e: ClassNotFoundException => log.error("Stopping brokers because of: {}", e); Stop
    case e: ActorInitializationException => log.error("Stopping brokers because of: {}", e); Stop
    case e: Exception => log.error("Caught exception: {}", e); Restart
  }

  def receive = {
    case (name: String, config: Config) => addBroker(name, config)
    case name: String => removeBroker(name)
    case Start => start()
    case msg => log.debug("can't handle {}", msg)
  }

  def start() {
    //Add camel brokers
    if(config.hasPath("evactor.brokers")) {
      config.getConfig("evactor.brokers").root.keySet.foreach{ k =>
        addBroker(k, config.getConfig("evactor.collectors").getConfig(k))
      }
    }
  }

  private[this] def addBroker(name: String, c: Config) {
    try {
      log.debug("adding broker with configuration: {}", c)
      val componentType = c.getString("type")
      val initMethod = c.getString("init")
      val brokerUri = c.getString("brokerUri")

      val component = dynamicAccess.getClassFor[Component](componentType).fold(throw _, p => p.getMethod(initMethod).invoke(brokerUri).asInstanceOf[Component])

      log.debug("{} broker added")

      //TODO: Handle creation of component by constructor as well
//      val component = initMethod match {
//        case Some(initMethod) => dynamicAccess.getClassFor(componentType).fold(throw _, p => p.getMethod(initMethod).invoke(brokerUri))
//        case None => dynamicAccess.createInstanceFor[Component](componentType, brokerUri).fold(throw _, p => p)
//      }

      camel.context.addComponent(name, component)
    } catch {
      case e: Exception => {
        log.warning("Adding component on broker {}, failed. {}", name, e)
        sender ! Status.Failure(e)
        throw e
      }
    }
  }

  private[this] def removeBroker(name: String) {
    camel.context.removeComponent(name)
  }

}
