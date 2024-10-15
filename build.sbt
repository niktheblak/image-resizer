name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.13.15"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  guice,
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.github.nscala-time" %% "nscala-time" % "2.34.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.8.7" % Test,
  "org.mockito" % "mockito-core" % "5.14.1" % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
