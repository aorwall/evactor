package org.evactor.monitor.ostrich

import com.twitter.ostrich.stats.Stats
import org.evactor.monitor.Monitoring
import akka.actor.ActorSystem

class OstrichMonitoring(system: ActorSystem) extends Monitoring {

  def incr(key: String, i: Int){
    if(i == 1){
      Stats.incr(key)
    } else {
      Stats.incr(key, i)
    }
  }
  
  def addLabel(key: String, value: String){
    Stats.setLabel(key, value)
  }
  
  def removeLabel(key: String){
    Stats.clearLabel(key)
  }
  
  def addMetric(key: String, value: Int){
    Stats.addMetric(key, value)
  }
  
  def removeMetric(key: String){
    Stats.removeMetric(key)
  }
  
  def time[T](key: String)(f: => T): T = {
    Stats.time(key)(f)
  }
  
  def addGauge(key: String, value: Double){
    Stats.addGauge(key)(value)
  }
  
  def removeGauge(key: String){
    Stats.clearGauge(key)
  }
  
}