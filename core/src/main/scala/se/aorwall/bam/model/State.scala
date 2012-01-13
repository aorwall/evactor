package se.aorwall.bam.model

object State {

  val TIMEOUT = 0

  val START = 1
  val RETRY = 2

  val SUCCESS = 10
  val INTERNAL_FAILURE = 11
  val BACKEND_FAILURE = 12
  val CLIENT_FAILURE = 13
  val UNKNOWN_FAILURE = 14

}