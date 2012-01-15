package se.aorwall.bam.model.statement

import akka.actor.Actor

abstract class Statement (val eventName: String, val statementId: String, val alertEndpoint: String) {

  def getStatementAnalyser: Actor

}