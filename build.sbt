import scalariform.formatter.preferences._

import AssemblyKeys._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.2",
	"com.typesafe.akka" %% "akka-slf4j" % "2.3.2",
	"com.typesafe.akka" %% "akka-testkit" % "2.3.2" % "test",
	"org.imgscalr" % "imgscalr-lib" % "4.2",
	"com.google.guava" % "guava" % "17.0",
	"com.google.code.findbugs" % "jsr305" % "2.0.1" % "provided",
	"org.apache.httpcomponents" % "httpclient" % "4.3.3",
	"joda-time" % "joda-time" % "2.3",
	"io.spray" % "spray-can" % "1.3.1",
	"io.spray" % "spray-routing" % "1.3.1",
	"org.mockito" % "mockito-all" % "1.9.5" % "test",
	"org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)

assemblySettings

mainClass in assembly := Some("org.ntb.imageresizer.service.SprayBootstrap")
