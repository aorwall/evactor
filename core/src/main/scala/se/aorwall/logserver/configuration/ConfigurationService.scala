package se.aorwall.logserver.configuration

import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.statement.Statement
import se.aorwall.logserver.process.ProcessActor
import se.aorwall.logserver.storage.{LogStorage, ConfigurationStorage}
import akka.actor.{Actor, TypedActor, ActorRef}
import akka.actor.Actor._
import akka.stm._
import se.aorwall.logserver.monitor.ActivityAnalyserPool
import collection.immutable.HashMap
import grizzled.slf4j.Logging

/**
 * Handling the configuration of business processes and monitored statements
 *
 * TODO: TypedActor!
 */
trait ConfigurationService {
  def addBusinessProcess(businessProcess: BusinessProcess): Unit

  def removeBusinessProcess(processId: String): Unit

  def addStatementToProcess(statement: Statement): Unit

  def removeStatementFromProcess(processId: String, statementId: String)
}

class ConfigurationServiceImpl(configurationStorage: ConfigurationStorage, logStorage: LogStorage) extends TypedActor with ConfigurationService with Logging {

  val activeProcesses = TransactionalMap[String, ActorRef]
  val activeStatementMonitors = TransactionalMap[String, Map[String, Statement]]

  val analyserPool = actorOf(new ActivityAnalyserPool)

  override def preStart = {
    trace("Starting ConfigurationService")
    // Read and start all processes and statements from configuration storage
    configurationStorage.readAllBusinessProcesses().foreach(p => startBusinessProcess(p))
    analyserPool.start()
  }

  override def postStop = {
    trace("Stopping ConfigurationService")
    val processesToStop = activeProcesses.map(_._1)
    trace("Will stop " + processesToStop.size + " active processes")
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
      processActor.start()
      activeProcesses += businessProcess.processId -> processActor
    }

    // Check for statements and start them to
    atomic {
      activeStatementMonitors += businessProcess.processId -> HashMap[String, Statement]()
    }

    configurationStorage.readStatements(businessProcess.processId).foreach(stmt => startStatement(stmt))
  }

  /**
   * start statement monitor
   */
  private def startStatement(statement: Statement) = {

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
      activeProcesses(processId).stop
      activeProcesses -= processId

      // Check for active statement montitors and stop them to
      val statementMonitors = activeStatementMonitors(processId)
      trace("Will stop " + statementMonitors.size + " active statement monitors for process " + processId)
      statementMonitors.map(_._2).foreach(_.stopMonitor())
      activeStatementMonitors -= processId
    }
  }

  def addStatementToProcess(statement: Statement): Unit = {
    configurationStorage.storeStatement(statement)
    startStatement(statement)
  }

  def removeStatementFromProcess(processId: String, statementId: String): Unit = {
    configurationStorage.deleteStatement(processId, statementId)
    atomic {
       activeStatementMonitors(processId)(statementId).stopMonitor()
       activeStatementMonitors(processId) -= statementId
     }
  }
}