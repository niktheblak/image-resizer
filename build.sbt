name := "image-resizer"
 
version := "1.0.0-SNAPSHOT"
 
scalaVersion := "2.10.0"

EclipseKeys.withSource := true

scalacOptions += "-feature"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"
 
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.1"

libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.7"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.1.1" % "test"

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

libraryDependencies += "com.google.guava" % "guava" % "13.0.1"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.2.1"

libraryDependencies += "commons-codec" % "commons-codec" % "1.7"

libraryDependencies += "joda-time" % "joda-time" % "2.1"

libraryDependencies += "io.spray" % "spray-can" % "1.1-M7"

libraryDependencies += "io.spray" % "spray-routing" % "1.1-M7"

libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"
  