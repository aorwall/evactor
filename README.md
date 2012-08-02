Evactor
=====================
***Check out the [example](https://github.com/aorwall/evactor-twitter) module to get a better understanding on how all this works.***

This is an attempt to create a complex event processing implementation in Akka. The idea is that *processors* subscribe to *channels* to receive *events* published on the channels. The event processor can then process the events in some way and publish new events on other event channels. 

The project also houses a storage solution, based on Apache Cassandra, for auditing and statistics. An API exists for easy access to historic data.


Flow
---------------------

### Event
*TODO*

### Collector
A *collector* collects events from external event producers. 

### Processor
A processor is a component that performs operations on events. This could be to extract data, create new events based on aggregated events or examining a collection of events to find a particular pattern.

To receive events the processor subscribes to channels or to all events that flows through the system. When the processor creates a new event it can publish it to another channel.

### Channel
*TODO*


Configuration
---------------------
Everything is configured in the `application.conf` file.

### Collectors
*TODO*

### Processors

#### Routers
Handles routing in the system

##### Forwarder
Just forwarding an event to a different channel (and category)

###### Configuration
```text
forwarderName {
  type = forwarder
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" }
}
```

##### Filter
Filtering out events that doesn't comply with a specified rule(expression)

###### Configuration
```text
filterName {
  type = filter
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" } 
  expression = { mvel = "value > 0" }
  accept = false
}
```

#### Analysers
Analysing sequences of events and produces new events with the result 

##### Count analyser
Counts events within a specified time frame

###### Configuration
```text
filterName {
  type = countAnalyser
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" } 
  categorize = true
  timeframe = 2 hours
}
```
##### Average analyser
Count average in a specified window (time or length)

###### Configuration
```text
averageAnalyserName {
  type = averageAnalyser
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" }
  categorize = false
  expression = { static = "foo" }
  window = { time = 1 minute }
}
```
##### Regression analyser
Calculates the regression coefficient within a specified time frame

###### Configuration
```text
regressionName {
  type = regressionAnalyser
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" } 
  categorize = true
  minSize = 25
  timeframe = 15 minutes
}
```

#### Alerter
Alerts when events that doesn't comply with a specified rule(expression) and informs when state is back to normal.

```text
  type = alerter
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" } 
  categorize = false
  expression = { mvel = "true" }
```

#### Builders
*TODO*

#### Producers
Produces events to external consumers.

##### LogProducers
Writes events to log.

###### Configuration
```text
logger {
  type = logProducer
  subscriptions = [ {channel = "foo"} ]
  loglevel = INFO
}
```

#### Custom processor
It's possible to create a custom processor and use it in the configuration by specifying the class instead of *type*.

##### Configuration
```text
customProcessor {
  class = org.example.CustomProcessor
  subscriptions = [ {channel = "foo"} ]
  publication = { channel = "bar" } 
  arguments = ["foo", "bar"]
}
```

##### Example implementation
```scala

class SimpleProcessor (
    override val subscriptions: List[Subscription],
    val publication: Publication)
  extends Processor(subscriptions) with Publisher {

  def process(event: Event) {
    // do stuff

    publish(event)
  }
}

```


Storage
---------------------
*TODO*

API
---------------------
*TODO*

Build and deploy
---------------------
*TODO*

Licence
---------------------
Copyright 2012 Albert Örwall

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
