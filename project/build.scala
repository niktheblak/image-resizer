import sbt._
import Keys._
import org.scalatra.sbt._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object ImageResizerBuild extends Build {
  val Organization = "org.ntb.imageresizer"
  val Name = "image-resizer"
  val Version = "1.0.0-SNAPSHOT"
  val ScalaVersion = "2.10.2"
  val ScalatraVersion = "2.2.1"

  lazy val project = Project (
    "image-resizer",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      ),
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.1",
      libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.1.1" % "test",
      libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2",
      libraryDependencies += "com.google.guava" % "guava" % "14.0.1",
      libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1",
      libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.2.1",
      libraryDependencies += "commons-codec" % "commons-codec" % "1.7",
      libraryDependencies += "joda-time" % "joda-time" % "2.1",
      libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  )
}
