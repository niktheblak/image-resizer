import scalariform.formatter.preferences._

import AssemblyKeys._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.10",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.10",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.10" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.google.guava" % "guava" % "18.0",
  "com.google.code.findbugs" % "jsr305" % "2.0.1" % "provided",
  "org.apache.httpcomponents" % "httpclient" % "4.4.1",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)

assemblySettings

mainClass in assembly := Some("org.ntb.imageresizer.service.SprayBootstrap")
