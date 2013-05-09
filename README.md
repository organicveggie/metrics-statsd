# metrics-statsd

Statsd reporter for codahale/metrics.

## Quick Start

```java
MetricsRegistry registry = new MetricsRegistry();
StatsdReporter reporter = new StatsdReporter(registry, "statsd.example.com", 8125);
reporter.start(15, TimeUnit.SECONDS);
```

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
