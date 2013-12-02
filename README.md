# metrics-statsd

Statsd reporter for [codahale/metrics] (https://github.com/codahale/metrics).

## Quick Start

The 3.x version of the Metrics library now uses the builder pattern to construct reporters. Below is an example of how to
create a StatsdReporter and report out metrics every 15 seconds.

 ```java
 final Statsd statsd = new Statsd("localhost", port);

 StatsdReporter reporter StatsdReporter.forRegistry(registry)
         .prefixedWith("foo")
         .convertDurationsTo(TimeUnit.MILLISECONDS)
         .convertRatesTo(TimeUnit.SECONDS)
         .filter(MetricFilter.ALL)
         .build(statsd);
reporter.start(15, TimeUnit.SECONDS);
```

If you are using Dropwizard (0.7.x +), there is an easy way to configure a Metrics reporter to be used directly from your
YAML configuration file. See (https://github.com/dropwizard/dropwizard/tree/master/dropwizard-metrics-graphite) for an
example of how to create a ReporterFactory.

## Important Notes

### Package Name & GroupId

As part of getting _metrics-statsd_ ready for publishing to Maven Central, the Maven _groupId_ changed from `studyblue` to `com.bealetech`. In addition, as of v2.3.0, the package name changed from `com.studyblue` to `com.bealetech`.

### Version Numbers

Versions 2.1.3.0 and earlier directly matched the versioning of official releases of [codahale/metrics](https://github.com/codahale/metrics). Future versions, starting with 2.2.0, will no longer match.

# Setting Up Maven
## Maven Repositories

Snapshots: [https://oss.sonatype.org/content/repositories/snapshots](https://oss.sonatype.org/content/repositories/snapshots)  
Releases: [https://oss.sonatype.org/content/groups/public](https://oss.sonatype.org/content/groups/public)

## Dependency

```xml
<dependencies>
    <dependency>
        <groupId>com.bealetech</groupId>
        <artifactId>metrics-statsd</artifactId>
        <version>${metrics-statsd.version}</version>
    </dependency>
</dependencies>
```

# Compatability with metrics

<table>
  <tr>
    <td><strong>metrics-statsd version</strong></td> 
    <td><strong>metrics version</strong></td> 
  </tr>
  <tr>
    <td>3.0.x</td>
    <td>3.0.1</td>
  </tr>
  <tr>
  	<td>2.3.x</td>
  	<td>2.1.x</td>
  </tr>  
  <tr>
  	<td>2.2.x</td>
  	<td>2.1.x</td>
  </tr>  
  <tr>
  	<td>2.1.3</td>
  	<td>2.1.x</td>
  </tr>  
</table>


# License

Copyright (c) 2012-2013 Sean Laurent

Published under Apache Software License 2.0, see LICENSE
