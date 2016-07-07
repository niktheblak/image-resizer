import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
