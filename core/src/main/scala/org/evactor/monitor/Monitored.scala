/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.monitor

import akka.actor.Actor
import akka.actor.ActorLogging

/**
 * Using Twitter Ostrich for monitoring. Will be changed to 
 * a configurable extension later...
 */
trait Monitored extends Actor with ActorLogging {

  val monitor = MonitoringExtension(context.system).getMonitoring
  if(monitor.isEmpty) log.warning("No monitoring implementation found")
  
  private val label = (key: String) => {
    "%s:%s".format(context.self.path, key)
  }
  
  def incr(key: String) {
    if(monitor.isDefined) monitor.get.incr(label(key), 1)
  }
  
  def incr(key: String, i: Int){
    if(monitor.isDefined) monitor.get.incr(label(key), i)
  }
  
  def addLabel(key: String, value: String){
    if(monitor.isDefined) monitor.get.addLabel(label(key), value)
  }
  
  def addLabel(value: String){
    if(monitor.isDefined) monitor.get.addLabel(context.self.path.toString, value)
  }
  
  def removeLabel(key: String){
    if(monitor.isDefined) monitor.get.removeLabel(label(key))
  }
  
  def removeLabel() {
    if(monitor.isDefined) monitor.get.removeLabel(context.self.path.toString)
  }
  def addMetric(key: String, value: Int){
    if(monitor.isDefined) monitor.get.addMetric(label(key), value)
  }
  
  def removeMetric(key: String){
    if(monitor.isDefined) monitor.get.removeMetric(label(key))
  }
  
  def addGauge(key: String, value: Double){
    if(monitor.isDefined) monitor.get.addGauge(label(key), value)
  }
  
  def removeGauge(key: String){
    if(monitor.isDefined) monitor.get.removeGauge(key)
  }
  
  def time[T](key: String)(f: => T): T = {
    if(monitor.isDefined) monitor.get.time(label(key))(f)
    else f
  }
  
}