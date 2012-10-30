name := "image-resizer"
 
version := "1.0.0-SNAPSHOT"
 
scalaVersion := "2.9.2"

EclipseKeys.withSource := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.3"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.3"

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

libraryDependencies += "com.google.guava" % "guava" % "11.0.2"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.2.1"

libraryDependencies += "commons-codec" % "commons-codec" % "1.7"

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "org.mockito" % "mockito-all" % "1.9.0"

libraryDependencies += "org.specs2" % "specs2_2.9.2" % "1.12.1"

parallelExecution in Test := false
  