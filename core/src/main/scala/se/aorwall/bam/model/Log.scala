package se.aorwall.bam.model

case class Log(
	server: String,
	componentId: String,
	correlationId: String,
	client: String,
	timestamp: Long,
	state: Int,     // TODO: Change state to enum...
	message: String
)