package se.aorwall.bam.storage

import se.aorwall.bam.model.process.BusinessProcess
import se.aorwall.bam.model.statement.Statement

trait ConfigurationStorage {

  def readAllBusinessProcesses(): List[BusinessProcess]
  def readBusinessProcess(processId: String): BusinessProcess
  def storeBusinessProcess(process: BusinessProcess)
  def deleteBusinessProcess(processId: String)

  def readStatements(processId: String): List[Statement]
  def readStatement(processId: String, statementId: String): Statement
  def storeStatement(statement: Statement)
  def deleteStatement(processId: String, statementId: String)

}