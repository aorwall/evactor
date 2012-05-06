package org.evactor.monitor

import akka.actor.Actor
import com.twitter.ostrich.stats.Stats

/**
 * Using Twitter Ostrich for monitoring. Will be changed to 
 * a configurable extension later...
 */
trait Monitored extends Actor {

  private val label = (key: String) => {
    "%s:%s".format(context.self.path, key)
  }
  
  def incr(key: String) {
    Stats.incr(label(key))
  }
  
  def incr(key: String, i: Int){
    Stats.incr(label(key), i)
  }
  
  def addLabel(key: String, value: String){
    Stats.setLabel(label(key), value)
  }
  
  def addLabel(value: String){
    Stats.setLabel(context.self.path.toString, value)
  }
  
  def removeLabel(key: String){
    Stats.clearLabel(label(key))
  }
  
  def removeLabel() {
    Stats.clearLabel(context.self.path.toString)
  }
  def addMetric(key: String, value: Int){
    Stats.addMetric(label(key), value)
  }
  
  def removeMetric(key: String){
    Stats.removeMetric(label(key))
  }
  
  def time[T](key: String)(f: => T): T = {
    Stats.time(label(key))(f)
  }
  
}