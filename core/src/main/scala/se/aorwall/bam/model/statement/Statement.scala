package se.aorwall.bam.model.statement

import akka.actor.Actor

abstract class Statement (val processId: String, val statementId: String, val alertEndpoint: String) {

  def getStatementAnalyser: Actor

}