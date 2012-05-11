package org.evactor.publish

import org.evactor.EvactorException

case class PublishException (msg: String) extends EvactorException(msg)