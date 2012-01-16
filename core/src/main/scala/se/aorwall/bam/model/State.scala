package se.aorwall.bam.model

sealed trait State { def name: String }

case object Start extends State { val name = "START" } 

case object Retry extends State { val name = "RETRY" }

case object Success extends State { val name = "SUCCESS" }

case object Failure extends State { val name = "FAILURE" }

case object Cancellation extends State { val name = "CANCELLATION" }

case object Timeout extends State { val name = "TIMEOUT" }
