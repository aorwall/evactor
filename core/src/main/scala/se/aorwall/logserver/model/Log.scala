package se.aorwall.logserver.model

case class Log(
	val server: String,
	val componentId: String,
	val correlationId: String,
	val client: String,
	val timestamp: Long,
	val state: Int,     // TODO: Change state to enum...
	val message: String
)