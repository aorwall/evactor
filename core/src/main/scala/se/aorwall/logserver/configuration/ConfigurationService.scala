package se.aorwall.logserver.configuration

import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.statement.Statement
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.storage.{LogStorage, ConfigurationStorage}
import akka.actor.{TypedActor, ActorRef}
import akka.actor.Actor._
import akka.stm._
import se.aorwall.logserver.monitor.ActivityAnalyserPool
import collection.immutable.HashMap
import grizzled.slf4j.Logging
import com.google.inject.Inject
import akka.config.Supervision.OneForOneStrategy

/**
 * Handling the configuration of business processes and monitored statements
 *
 * TODO: TypedActor!
 */
trait ConfigurationService {
  def addBusinessProcess(businessProcess: BusinessProcess)

  def removeBusinessProcess(processId: String)

  def addStatementToProcess(statement: Statement)

  def removeStatementFromProcess(processId: String, statementId: String)
}

class ConfigurationServiceImpl() extends TypedActor with ConfigurationService with Logging {
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 3, 5000)

  var configurationStorage: Option[ConfigurationStorage] = None
  var logStorage: Option[LogStorage] = None

  @Inject
  def setConfigurationStorage(tConfigurationStorage: ConfigurationStorage) {
    // Read and start all processes and statements from configuration storage when it gets injected
    tConfigurationStorage.readAllBusinessProcesses().foreach(p => startBusinessProcess(p))
    configurationStorage = Some(tConfigurationStorage)
  }

  @Inject
  def setLogStorage(tLogStorage: LogStorage) {
     info("setting log storage")
     logStorage = Some(tLogStorage)
  }

  val activeProcesses = TransactionalMap[String, ActorRef]()
  val activeStatementMonitors = TransactionalMap[String, Map[String, Statement]]()

  val analyserPool = actorOf(new ActivityAnalyserPool)

  override def preStart() {
    debug("Starting ConfigurationService")
    self.startLink(analyserPool)
  }

  override def postStop() {
    debug("Stopping ConfigurationService")
    val processesToStop = activeProcesses.map(_._1)
    trace("Will stop " + processesToStop.size + " active processes")
    processesToStop.foreach(p => stopBusinessProcess(p))
    analyserPool.stop()
  }

  /**
   * Saves process configuration in storage and starts an process actor
   */
  def addBusinessProcess(businessProcess: BusinessProcess) {
    configurationStorage match {
      case Some(c) => c.storeBusinessProcess(businessProcess)
      case None =>
    }
    startBusinessProcess(businessProcess)
  }

  /**
   * Remove process configuration in storage and stop the related process actor
   */
  def removeBusinessProcess(processId: String) {
    configurationStorage match {
      case Some(c) => c.deleteBusinessProcess(processId)
      case None =>
    }
    stopBusinessProcess(processId)
  }

  /**
   * start process
   */
  private def startBusinessProcess(businessProcess: BusinessProcess) {
    val processActor = actorOf(new ProcessActor(businessProcess, logStorage, analyserPool))
    atomic {
      self.startLink(processActor.start())
      activeProcesses += businessProcess.processId -> processActor
    }

    // Check for statements and start them to
    atomic {
      activeStatementMonitors += businessProcess.processId -> HashMap[String, Statement]()
    }

    configurationStorage match {
      case Some(c) => c.readStatements(businessProcess.processId).foreach(stmt => startStatement(stmt))
      case None =>
    }
  }

  /**
   * start statement monitor
   */
  private def startStatement(statement: Statement) {
    atomic {
      statement.startMonitor()
      activeStatementMonitors(statement.processId) += statement.statementId -> statement
    }
  }

  /**
   * stop process
   */
  private def stopBusinessProcess(processId: String) = {
    atomic {
      self.unlink(activeProcesses(processId))
      activeProcesses(processId).stop()
      activeProcesses -= processId

      // Check for active statement montitors and stop them to
      val statementMonitors = activeStatementMonitors(processId)
      trace("Will stop " + statementMonitors.size + " active statement monitors for process " + processId)
      statementMonitors.map(_._2).foreach(_.stopMonitor())
      activeStatementMonitors -= processId
    }
  }

  def addStatementToProcess(statement: Statement) {
    configurationStorage match {
      case Some(c) => c.storeStatement(statement)
      case None =>
    }
    startStatement(statement)
  }

  def removeStatementFromProcess(processId: String, statementId: String) {
    configurationStorage match {
      case Some(c) => c.deleteStatement(processId, statementId)
      case None =>
    }
    atomic {
       activeStatementMonitors(processId)(statementId).stopMonitor()
       activeStatementMonitors(processId) -= statementId
     }
  }
}