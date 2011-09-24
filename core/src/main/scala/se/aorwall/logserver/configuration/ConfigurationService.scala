package se.aorwall.logserver.configuration

import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.statement.Statement
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.storage.{LogStorage, ConfigurationStorage}
import akka.actor.{Actor, TypedActor, ActorRef}
import akka.actor.Actor._
import akka.stm._
import se.aorwall.logserver.monitor.ActivityAnalyserPool

/**
 * Handling the configuration of business processes and monitored statements
 *
 * TODO: TypedActor!
 */
trait ConfigurationService {
  def addBusinessProcess(businessProcess: BusinessProcess): Unit

  def removeBusinessProcess(processId: String): Unit

  def addStatementToProcess(processId: String, statement: Statement): Unit

  def removeStatementFromProcess(processId: String, statementId: String)
}

class ConfigurationServiceImpl(configurationStorage: ConfigurationStorage, logStorage: LogStorage) extends TypedActor with ConfigurationService {

  val activeProcesses = TransactionalMap[String, ActorRef]
  val analyserPool = actorOf(new ActivityAnalyserPool)

  override def preStart = {
    // Read and start all processes and statements from configuration storage
    configurationStorage.readAllBusinessProcesses().foreach(p => startBusinessProcess(p))
    analyserPool.start()
  }

  override def postStop = {
    val processesToStop = activeProcesses.map(_._1)
    processesToStop.foreach(p => stopBusinessProcess(p))
    analyserPool.stop()
  }

  /**
   * Saves process configuration in storage and starts an process actor
   */
  def addBusinessProcess(businessProcess: BusinessProcess): Unit = {
    configurationStorage.storeBusinessProcess(businessProcess)
    startBusinessProcess(businessProcess)
  }

  /**
   * Remove process configuration in storage and stop the related process actor
   */
  def removeBusinessProcess(processId: String): Unit = {
    configurationStorage.deleteBusinessProcess(processId)
    stopBusinessProcess(processId)
  }

  /**
   * start process
   */
  private def startBusinessProcess(businessProcess: BusinessProcess) = {
    val processActor = actorOf(new ProcessActor(businessProcess, logStorage, analyserPool))
    atomic {
      activeProcesses += businessProcess.processId -> processActor
    }

    // Check for statements and start them to
    configurationStorage.readStatements(businessProcess.processId).foreach(_.createActor(businessProcess.processId).start)
  }

  /**
   * stop process
   */
  private def stopBusinessProcess(processId: String) = {
    activeProcesses(processId).stop

    atomic {
      activeProcesses -= processId
    }

    // Check for active statement montitors and stop them to
    val statementMonitors = Actor.registry.actorsFor(processId)
    statementMonitors.foreach(_.stop())
  }

  def addStatementToProcess(processId: String, statement: Statement): Unit = {
    configurationStorage.storeStatement(processId, statement)
    val statementMonitor = statement.createActor(processId)
    statementMonitor.id = processId + ":" + statement.statementId
    statementMonitor.start()
  }

  def removeStatementFromProcess(processId: String, statementId: String): Unit = {
    configurationStorage.deleteStatement(processId, statementId)
    val statementMonitors = Actor.registry.actorsFor(processId + ":" + statementId)
    statementMonitors.foreach(_.stop())
  }

}