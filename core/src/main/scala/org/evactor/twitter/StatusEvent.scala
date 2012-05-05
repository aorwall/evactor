package org.evactor.twitter

import org.evactor.model.events.Event
import org.evactor.model.attributes.HasMessage

case class StatusEvent (
    override val id: String,
    override val timestamp: Long,
    val screenName: String,
    val message: String,
    val urls: List[String],
    val hashtags: List[String]) extends Event (id, timestamp) with HasMessage {

}