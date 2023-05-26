name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.17"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  guice,
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.github.nscala-time" %% "nscala-time" % "2.22.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.22" % Test,
  "org.mockito" % "mockito-core" % "2.28.2" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
