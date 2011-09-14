package se.aorwall.logserver.model

case class LogEvent (val correlationId: String,
                val componentId: String,
                val timestamp: Long,
                val state: Int)