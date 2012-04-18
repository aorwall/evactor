package se.aorwall.bam.utils

object JavaHelpers {  
  
  /**
   * Convert nullable variables to Option
   */
  implicit def any2option[T <: AnyRef](any: T): Option[T] = {
    if(any == null){
      None
    } else {
      Some(any)
    }
    
  }
}