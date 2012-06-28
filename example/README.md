Twitter Example
====================
This is an example implementation of Evactor that analyses status updates on Twitter. It receives tweets from the [Twitter Streaming API](https://dev.twitter.com/docs/streaming-api). 

By using Evactor to process tweets as events, we can find patterns and/or determine odd behaviour in real time. In this example we will look for popular URL's, trending hashtags and alert when such events occurs. By enabling the API and Storage modules it will also be possible to go back and look at historic data and statistics.

Installation
---------------------

### Build from source

Install [sbt](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup) if you don't already have it. The application has been tested with sbt 0.11.3.

Clone the Evactor repo: `git clone git://github.com/aorwall/evactor.git`

Replace `[TWITTER_USERNAME]` and `[TWITTER_PASSWORD]` your own Twitter username and password in: `evactor/example/src/main/resources/application.conf`

Run *sbt* and build the example jar:
```text
$ sbt
> project example
> assembly
> exit
```

An executable jar file can now be found in `evactor/example/target` and is executed with the command `java -jar evactorExample.jar`. 

See [Cassandra storage] and [API] for instructions on how to enable the storage and API modules.

Understand the configuration
---------------------
All the components that runs on Evactor can be configured in the configuration file ([application.conf](https://github.com/aorwall/evactor/blob/master/example/src/main/resources/application.conf)). Therefore it's a good start to examine the example configuration to get an understanding on how it all hangs together.

This example has two main flows, one for finding trending hashtags and one to look for popular URL:s.

### Collecting events
To get events into the Evactor event stream we need a [*collector*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/collect/Collector.scala). The collector is connected to a *listener*, used to receive events from external sources, and a *transformer* that transforms events in external formats to an internal event object. In this example, the collector receives status updates from Twitter's 'Spritzer' API (1% of all status updates on Twitter) and publishes them to the channel `twitter`. 

#### Collector flow
1. A custom [listener class](https://github.com/aorwall/evactor/blob/master/example/src/main/scala/org/evactor/twitter/listener/TwitterListener.scala) listens for new tweets on `https://stream.twitter.com/1/statuses/sample.json`. 
2. When the listener receives a tweet, in json format, it sends the tweet to a custom [transformer](https://github.com/aorwall/evactor/blob/master/example/src/main/scala/org/evactor/twitter/transformer/TwitterJsonToStatusEvent.scala) that transforms the Json message to a [StatusEvent](https://github.com/aorwall/evactor/blob/master/example/src/main/scala/org/evactor/twitter/StatusEvent.scala).
3. The transformers send the message to the collector that checks for duplicates and publishes the StatusEvent to the channel `twitter`.

#### The configuration
> To use the Twitter Stream API, you must have a Twitter account and replace *[TWITTER_USERNAME]* and *[TWITTER_PASSWORD]* with your own credentials.

```text
twitter_collector {
  # CHANGE [TWITTER_USERNAME] AND [TWITTER_PASSWORD] TO YOUR OWN TWITTER CREDENTIALS!
  listener {
    class = "org.evactor.twitter.listener.TwitterListener"
    arguments = [ "https://stream.twitter.com/1/statuses/sample.json", "[TWITTER_USERNAME]", "[TWITTER_PASSWORD]" ]
  }
  transformer {
    class = "org.evactor.twitter.transformer.TwitterJsonToStatusEvent"
  }
  publication = { channel = "twitter" }
}
```

### Trending hashtags
Look for trending hashtags by analysing events with a specific hashtag that more than doubles in a 15 minutes time.

#### Filter 
A [*Filter*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/route/Filter.scala) subscribes to `twitter` and filters out    all status updates not containing hashtags and publishes the rest to the channel `twitter:hashtag` categorized by hashtag.

[mvel](http://mvel.codehaus.org/) is used to examine the content of the event.

`hashtags == null || hashtags.size() == 0` declares that the hashtag list in the StatusEvent object is null or doesn't contain any elements, *true* is returned and the Filter will not forward the event.

In *publication* the mvel expression `hashtags` declares that the event category on the event will be set to all hashtags specified in the event.

`accept = false` specifies that the filter should filter out all events that corresponds with the expression.

```text
twitter_hashtag_filter {
  type = filter 
  subscriptions = [ {channel = "twitter"} ]
  publication = { channel = "twitter:hashtag", categories = { mvel = "hashtags" } }
  expression = { mvel = "hashtags == null || hashtags.size() == 0" } 
  accept = false
}
```

#### Regression analyser
A [*Regression analyser*]() analyses events categorized by hashtag and creates a *ValueEvent* with the regression coefficient for events from the last 15 minutes.
    
> "coefficient" is supposed to represent the regression coefficient. Maybe not totally scientifically correct, but this means that the number of events grows with more than [coefficient]*[no event] under the specified time frame. A coefficient with a negative value indicates that the number of arriving event is declining.

```text
twitter_hashtag_trend {
  type = regressionAnalyser
  subscriptions = [ { channel = "twitter:hashtag" } ]
  publication = { channel = "twitter:hashtag:trend" }
  categorize = true
  minSize = 25
  timeframe = 15 minutes
}

```

#### Alerter
A *alerter* listens to `twitter:hashtag:trend` and alerts when the regression coefficient exceeds 1.0.

```text
twitter_hashtag_trending {
  type = alerter
  subscriptions = [ { channel = "twitter:hashtag:trend" } ]
  publication = { channel = "twitter:hashtag:trending" }
  categorize = true
  expression = { mvel = "value > 1.0" }
}
```

#### Log producer
Logs alerts

```text
log_trending_hashtags {
  type = logProducer
  subscriptions = [ { channel = "twitter:hashtag:trending" } ]
  loglevel = INFO
}
```

### Popular URL's
Look for URL's that are retweeded more than 50 times in an hour.

#### Filters
The filtering is done in two steps in the URL flow. First we filter out all tweets that isn't a retweet and sends the rest to `twitter:retweet`. Then we filter out tweets not containing an URL and send the rest to `twitter:url` categorised by URL.

##### Retweet filter
```text
twitter_rt_filter {
  type = filter
  subscriptions = [ {channel = "twitter"} ]
  publication = { channel = "twitter:rt" }
  expression = { mvel = "!message.startsWith('RT')" }
  accept = false
}
```

##### URL filter
```text
twitter_url_filter {
  type = filter
  subscriptions = [ {channel = "twitter:rt"} ]
  publication = { channel = "twitter:url", categories = { mvel = "urls"} }
  expression = { mvel = "urls == null || urls.size() == 0" }
  accept = false
}
```

#### Count analyser
A [*Count analyser*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/analyse/count/CountAnalyser.scala) subscribes to `twitter:url` and sends a ValueEvent with the current number of events of a category from the last 30 minutes to  `twitter:url:count`.

```text
twitter_url_popular {
  type = countAnalyser
  subscriptions = [ { channel = "twitter:rt" } ]
  publication = { channel = "twitter:url:count" }
  categorize = true
  timeframe = 30 minutes
}
```

#### Alerter
A *alerter* listens to `twitter:hashtag:count` and alerts when the count exceeds 5.

```text
twitter_url_popular {
  type = alerter
  subscriptions = [ { channel = "twitter:url:count" } ]
  publication = { channel = "twitter:url:popular" }
  categorize = true
  expression = { mvel = "value > 5" }
}
```

#### Log producer
Logs alert

```text
log_popular_urls {
  type = logProducer
  subscriptions = [ { channel = "twitter:url:popular" } ]
  loglevel = INFO
}
```

### Log tweets about Scala, Akka and Cassandra
An *LogProducer* subscribes to `twitter:hashtag` and the categories `scala`, `cassandra` and `akka`.

> This won't happen very often because the Spritzer stream just gives us 1% of all tweets. If you want to receive all tweets with these hashtags, try the following URL: https://stream.twitter.com/1/statuses/filter.json?track=scala,cassandra,akka

```text
log_cool_hashtags {
  type = logProducer
  subscriptions = [ 
    { channel = "twitter:hashtag", category = "akka" },
    { channel = "twitter:hashtag", category = "scala" },
    { channel = "twitter:hashtag", category = "cassandra" }]
  loglevel = INFO
}
```

Apache Cassandra
---------------------
Instructions on how to store events and statistics to an Apache Cassandra database.


### Apache Cassandra
Install [Apache Cassandra](http://cassandra.apache.org/). Instructions is found [here](http://wiki.apache.org/cassandra/GettingStarted)

Add the column families to Cassandra with *cassandra-cli*:
```text
create keyspace Evactor;
use Evactor;
create column family Channel with default_validation_class=CounterColumnType and comparator = UTF8Type and replicate_on_write=true;
create column family Category with default_validation_class=CounterColumnType and comparator = UTF8Type and replicate_on_write=true;
create column family Event with comparator = UTF8Type;
create column family Timeline with comparator = UUIDType;
create column family Statistics with default_validation_class=CounterColumnType and comparator = LongType and replicate_on_write=true;
create column family 'Index' with default_validation_class=CounterColumnType and comparator = UTF8Type and replicate_on_write=true;
create column family Latency with default_validation_class=CounterColumnType and comparator = LongType and replicate_on_write=true;
create column family KpiSum with comparator = LongType;
``` 

Decomment the following section in application.conf:
```text
storage {
    
  implementation = "org.evactor.storage.cassandra.CassandraStorage"
    
  channels = all

  cassandra {
    hostname = "localhost"
    port = 9160
    clustername = "ClusterTest"
    keyspace = "Evactor"
  }
}
```

Evactor will now store events and statistics about events published on all channels. Use the API to browse through stored data.


API
---------------------

### Configuration
Enable the API by decommenting the following section in `application.conf`:

```text
api {
  port = 8080
}
```

### Methods

#### Channels
URL: http://host:port/api/channels

Response:


#### Categories

URL: http://host:port/api/categories/[channel]

Response:

#### Event timeline

URL: http://host:port/api/timeline/\[channel\](/[category])
Parameters:

Response:


#### Event
URL: http://host:port/api/event/\[id\]

Response:


#### Statistics

URL: http://host:port/api/stats/\[channel\](/[category])(?interval=[INTERVAL])
Parameters:
- Interval: HOUR, DAY, MONTH, YEAR

Response:


Monitoring
---------------------
Enable [Ostrich](https://github.com/twitter/ostrich) monitoring by decommenting the following section in `application.conf`:

```text
monitoring {
  ostrich {
    port = 8888
  }
}
```

Go to `http://host:port/stats.txt`


Licence
---------------------
Copyright 2012 Albert Örwall

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
