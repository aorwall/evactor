package se.aorwall.bam.model

object State {

  val TIMEOUT = 0

  val START = 1
  val RETRY = 2

  val SUCCESS = 10
  val FAILURE = 11
  val CANCELLATION = 12

}