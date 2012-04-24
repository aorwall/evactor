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
package org.evactor.process.alert

import org.evactor.process.Processor
import akka.actor.ActorLogging
import org.evactor.process.Subscription

/**
 * Alerter sends alerts about events to external parties and
 * should not create new events.
 */
abstract class Alerter (
    override val subscriptions: List[Subscription]) 
  extends Processor (subscriptions)
  with ActorLogging {
  
}
