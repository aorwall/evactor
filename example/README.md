Twitter Example
====================
This is an example implementation that analyses and stores status updates 
from Twitter. It uses the Twitter Stream API and to run the application 
one must specify a Twitter username and password in the application.conf.

Flow
---------------------
1.  A *Collector* receives status uploads from Twitters 'Spritzer' API (1% 
    of all status updates on Twitter) and publishes them on the channel 
    `[twitter]`.
    
2.  A *Filter processor* subscribes to `[twitter]` and filters out
    all status updates containing hashtags and publish them to the channel
    `[twitter:hashtag]` and categorize the events by hashtag.
    
3.  A *Counter analyser* subscribes to `[twitter:hashtag]` and publish an
    alert event to `[twitter:hashtag:popular]` when an event with the same 
    category arrives more than ten times within an hour.    
    
4. 'TODO:' A *Alerter* subscribes to `[twitter:hashtag]` and the specific 
    categories `scala`, `cassandra` and `akka` and sends alerts to external
    consumers.
    
5.  A *Filter processor* subscribes to `[twitter]` and filters out
    all status updates containing url's and publish them to the channel
    `[twitter:url]` and categorize the events by url.
    
3.2. A *Counter analyser* subscribes to `[twitter:url]` and alerts when an
     event with the same category arrives more than five times within an hour.    

Installation
---------------------
Apache Cassandra must be installed and running to run this example. It can be
found [here](http://http://cassandra.apache.org/). 

To run the application just run the command `dist` in *sbt* and then start 
the application by running the command: `bin/start org.evactor.ExampleKernel` in
`target/dist`.

An API server will also be started on port 8080 and a [Ostrich](https://github.com/twitter/ostrich) server.