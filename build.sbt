import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  guice,
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.3" % Test,
  "org.mockito" % "mockito-core" % "2.2.29" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
