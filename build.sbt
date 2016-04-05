name := "image-resizer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.15",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.15",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.15" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.google.guava" % "guava" % "19.0",
  "com.google.code.findbugs" % "jsr305" % "2.0.1" % "provided",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-client" % "1.3.3",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

mainClass in assembly := Some("org.ntb.imageresizer.service.SprayBootstrap")

assemblyJarName in assembly := "imageresizer.jar"

test in assembly := {}
