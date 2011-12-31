package se.aorwall.logserver.configuration

import se.aorwall.logserver.model.process.BusinessProcess
import se.aorwall.logserver.model.statement.Statement
import se.aorwall.logserver.storage.{LogStorage, ConfigurationStorage}
import grizzled.slf4j.Logging
import com.google.inject.Inject

/**
 * Handling the configuration of business processes and monitored statements
 */
trait ConfigurationService {
  def addBusinessProcess(businessProcess: BusinessProcess)

  def removeBusinessProcess(processId: String)

  def addStatementToProcess(statement: Statement)

  def removeStatementFromProcess(processId: String, statementId: String)
}

class ConfigurationServiceImpl() extends ConfigurationService with Logging {

  var configurationStorage: Option[ConfigurationStorage] = None
  var logStorage: Option[LogStorage] = None

  @Inject
  def setConfigurationStorage(tConfigurationStorage: ConfigurationStorage) {
    //TODO: Read and start all processes and statements from configuration storage when it gets injected

    configurationStorage = Some(tConfigurationStorage)
  }

  /**
   * Saves process configuration in storage and starts an process actor
   */
  def addBusinessProcess(businessProcess: BusinessProcess) {
    configurationStorage match {
      case Some(c) => c.storeBusinessProcess(businessProcess)
      case None =>
    }
  }

  /**
   * Remove process configuration in storage and stop the related process actor
   */
  def removeBusinessProcess(processId: String) {
    configurationStorage match {
      case Some(c) => c.deleteBusinessProcess(processId)
      case None =>
    }
  }


  def addStatementToProcess(statement: Statement) {
    configurationStorage match {
      case Some(c) => c.storeStatement(statement)
      case None =>
    }
  }

  def removeStatementFromProcess(processId: String, statementId: String) {}
}