package se.aorwall.bam.configuration

import se.aorwall.bam.model.statement.Statement
import grizzled.slf4j.Logging
import com.google.inject.Inject

/**
 * Handling the configuration of business processes and monitored statements
 */
trait ConfigurationService {
  def addProcess(process: String)

  def removeProcess(processId: String)

  def addStatement(statement: Statement)

  def removeStatement(statementId: String)
}

class ConfigurationServiceImpl() extends ConfigurationService with Logging {

  /**
   * Saves process configuration in storage and starts an process actor
   */
  def addProcess(process: String) {
	// Create 
  }

  /**
   * Remove process configuration in storage and stop the related process actor
   */
  def removeProcess(processId: String) {
    
  }


  def addStatement(statement: Statement) {
    
  }

  def removeStatement(statementId: String) {}
}