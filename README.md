# metrics-statsd

Statsd reporter for [codahale/metrics] (https://github.com/codahale/metrics), based on Sean Laurent's [metrics-statsd] (https://github.com/organicveggie/metrics-statsd), Mike Keesey's [metrics-statsd] (https://github.com/ReadyTalk/metrics-statsd), Hannes Heijkenskjöld [metrics-statsd] (https://github.com/jjagged/metrics-statsd) and Mayconbordin [metrics-statsd] (https://github.com/mayconbordin/metrics-statsd) with added support for the TCP protocol.

Only version 3.x of the Metrics library is supported.

## Integration

```scala
libraryDependencies += "io.github.morgaroth" %% metrics-statsd" % "1.5.1"
```

## Quick Start

The 3.x version of the Metrics library uses the builder pattern to construct reporters. Below is an example of how to
create a StatsdReporter and report out metrics every 15 seconds.

 ```java
 StatsDReporter reporter = StatsDReporter.forRegistry(registry)
         .prefixedWith("foo")
         .convertDurationsTo(TimeUnit.MILLISECONDS)
         .convertRatesTo(TimeUnit.SECONDS)
         .filter(MetricFilter.ALL)
         .build("localhost", 8125, "tcp");

reporter.start(15, TimeUnit.SECONDS);
```
