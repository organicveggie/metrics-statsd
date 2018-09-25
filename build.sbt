
//profileName := "morgaroth"

description := "Metrics Statsd Support"

name := "metrics-statsd"

version := "1.5.1"

organization := "io.github.morgaroth"

publishMavenStyle := true

libraryDependencies ++= Seq(
  "io.dropwizard.metrics"    % "metrics-core"     % "3.1.0",
  "com.google.code.findbugs" % "jsr305"           % "2.0.1",
  "org.slf4j"                % "slf4j-api"        % "1.7.7",
  "org.slf4j"                % "slf4j-jdk14"      % "1.7.7"  % "test",
  "junit"                    % "junit-dep"        % "4.10"   % "test",
  "org.mockito"              % "mockito-all"      % "1.9.0"  % "test",
  "org.easytesting"          % "fest-assert-core" % "2.0M10" % "test"
)

crossScalaVersions := Seq(
  "2.10.0"
  ,"2.11.0"
  ,"2.12.0"
)

pomExtra := <url>https://github.com/Morgaroth/morgaroth-utils</url>
  <licenses>
    <license>
      <name>Apache License 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/Morgaroth/metrics-statsd</url>
    <connection>https://github.com/Morgaroth/metrics-statsd</connection>
    <developerConnection>scm:git:git@github.com:Morgaroth/metrics-statsd.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>morgaroth</id>
      <name>Mateusz Jaje</name>
    </developer>
  </developers>
