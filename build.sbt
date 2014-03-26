import scalariform.formatter.preferences._

import AssemblyKeys._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.4"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.2.4"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.4" % "test"

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

libraryDependencies += "com.google.guava" % "guava" % "16.0.1"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1" % "provided"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.1"

libraryDependencies += "joda-time" % "joda-time" % "2.3"

libraryDependencies += "io.spray" % "spray-can" % "1.2.1"

libraryDependencies += "io.spray" % "spray-routing" % "1.2.1"

libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.0" % "test"

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)

assemblySettings

mainClass in assembly := Some("org.ntb.imageresizer.service.SprayBootstrap")
