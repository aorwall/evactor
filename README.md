This is an attempt to create a complex event processing implementation in Akka. The idea is that *processors* subscribe to *channels* to receive *events* published on the channels. The event processor can then process the events in some way and publish new events on other event channels. 

The project also houses a storage solution, based on Apache Cassandra, for auditing and statistics. An API exists for easy access to historic data.

There are also an example module that analyses data from Twitter's Stream API. 
