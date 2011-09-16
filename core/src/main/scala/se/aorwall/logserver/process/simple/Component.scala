package se.aorwall.logserver.model.process.simple

case class Component (val componentId: String, val maxRetries: Int) {

  override def equals(other: Any): Boolean = other match {
    case other: Component => componentId == other.componentId
    case _ => false
  }

  override def toString() = "Component (id: " + componentId + ", maxRetries: " + maxRetries + ")"

}