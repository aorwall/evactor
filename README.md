Evactor
=====================
***Check out the [example](https://github.com/aorwall/evactor/tree/master/example) module to get a better understanding on how all this works.***

This is an attempt to create a complex event processing implementation in Akka. The idea is that *processors* subscribe to *channels* to receive *events* published on the channels. The event processor can then process the events in some way and publish new events on other event channels. 

The project also houses a storage solution, based on Apache Cassandra, for auditing and statistics. An API exists for easy access to historic data.


Licence
---------------------
Copyright 2012 Albert Örwall

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0