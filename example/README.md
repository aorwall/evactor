Twitter Example
====================
This is an example implementation that analyses and stores status updates 
from Twitter. To receive tweets it uses the [Twitter Streaming API](https://dev.twitter.com/docs/streaming-api).

Flow
---------------------
The flow is configured in [ExampleKernel.scala](https://github.com/aorwall/evactor/blob/master/example/src/main/scala/org/evactor/ExampleKernel.scala).

1.  A *Collector* receives status uploads from Twitter's 'Spritzer' API (1% 
    of all status updates on Twitter) and publishes them to the channel `twitter`.
    
2.  A [*Filter*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/route/Filter.scala) subscribes to `twitter` and filters out
    all status updates containing hashtags and publish them to the channel
    `twitter:hashtag` and categorize the events by hashtag.
    
3.  A [*Count analyser*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/analyse/count/CountAnalyser.scala) subscribes to `twitter:hashtag` and publish an
    alert event to `twitter:hashtag:popular` when an event with the same 
    category arrives more than ten times within an hour.    
    
4.  *TODO:* An *Alerter* subscribes to `twitter:hashtag` and the categories
    `scala`, `cassandra` and `akka` and sends alerts to external consumers.
    
5.  A [*Filter*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/route/Filter.scala) subscribes to `twitter` and filters out
    all status updates containing url's and publish them to the channel
    `twitter:url` and categorize the events by url.
    
6.  A [*Count analyser*](https://github.com/aorwall/evactor/blob/master/core/src/main/scala/org/evactor/process/analyse/count/CountAnalyser.scala) subscribes to `twitter:url` and alerts when an
     event with the same category arrives more than five times within an hour.    

Installation
---------------------

### Apache Cassandra
Install [Apache Cassandra](http://wiki.apache.org/cassandra/GettingStarted)

Add the column families to Cassandra with *cassandra-cli*:
```text
create keyspace Evactor;
use Evactor;
create column family Channel with default_validation_class=CounterColumnType and comparator = UTF8Type and replicate_on_write=true;
create column family Category with default_validation_class=CounterColumnType and comparator = UTF8Type and replicate_on_write=true;
create column family Event with comparator = UTF8Type;
create column family Timeline with comparator = UUIDType;
create column family StateTimeline with comparator = UUIDType;
create column family Statistics with default_validation_class=CounterColumnType and comparator = LongType and replicate_on_write=true;
create column family Latency with default_validation_class=CounterColumnType and comparator = LongType and replicate_on_write=true;
create column family KpiSum with comparator = LongType;
``` 

### sbt
Install [sbt](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup)

### Install the evactor example

Clone the Evactor repository `git clone git@github.com:aorwall/evactor.git`

Set your own Twitter username and password in `evactor/example/src/main/resources/application.conf`

Run *sbt* and build the example jar:
```text
$ sbt
> project example
> assembly
> exit
```

A executable jar can now be found in evactor/example/target and is executed with the command `java -jar evactorExample.jar`. The server will now start listen for tweets. An API server will also be started on port 8080 and an [Ostrich](https://github.com/twitter/ostrich) server on port 8888.