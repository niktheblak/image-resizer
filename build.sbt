import scalariform.formatter.preferences._

import AssemblyKeys._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.7",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.google.guava" % "guava" % "18.0",
  "com.google.code.findbugs" % "jsr305" % "2.0.1" % "provided",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "joda-time" % "joda-time" % "2.3",
  "io.spray" %% "spray-can" % "1.3.2",
  "io.spray" %% "spray-routing" % "1.3.2",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)

assemblySettings

mainClass in assembly := Some("org.ntb.imageresizer.service.SprayBootstrap")
