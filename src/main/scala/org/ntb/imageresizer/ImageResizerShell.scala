package org.ntb.imageresizer

import actor.{ ResizeActor, ImageBrokerActor, DownloadActor }
import com.typesafe.config.ConfigFactory
import org.apache.http.HttpException
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import java.net.URI
import java.net.URISyntaxException

object ImageResizerShell extends App {
  import ImageBrokerActor._

  implicit val timeout: Timeout = 10.seconds
  val config = ConfigFactory.parseString("""
    akka {
      stdout-loglevel = "OFF"
      loglevel = "INFO"
    }
  """)
  println("Starting ImageResizer shell")
  val system = ActorSystem("ImageResizer", ConfigFactory.load(config))
  val resizeActor = system.actorOf(Props[ResizeActor], "resizer")
  val downloadActor = system.actorOf(Props[DownloadActor], "downloader")
  val imageBrokerActor = system.actorOf(Props(classOf[ImageBrokerActor], downloadActor, resizeActor), "imagebroker")
  processCommands()

  def processCommands() {
    try {
      while (true) {
        Console.print("> ")
        Console.flush()
        val command = Console.readLine()
        val tokens = command.split(' ').toList
        handleCommand(tokens)
      }
    } catch {
      case e: Exception ⇒
        e.printStackTrace()
        exit()
    }
  }

  def exit() {
    system.shutdown()
    sys.exit()
  }

  def handleCommand(tokens: List[String]) {
    tokens match {
      case "resize" :: path :: rest ⇒
        try {
          val size = if (rest.nonEmpty) rest.head.toInt else 128
          handleResizeCommand(path, size)
        } catch {
          case e: NumberFormatException ⇒ Console.err.println(s"Invalid argument for size: ${rest.mkString(" ")}")
          case e: URISyntaxException ⇒ Console.err.println(s"Invalid URL: $path}")
          case e: UnsupportedImageFormatException ⇒ Console.err.println(s"Unsupported image format for $path")
          case e: HttpException ⇒ Console.err.println(s"Could not download $path: ${e.getMessage}")
        }
      case "exit" :: Nil ⇒
        exit()
      case command ⇒
        Console.err.println(s"Invalid command or arguments: ${command.mkString(" ")}")
    }
  }

  def handleResizeCommand(path: String, size: Int) {
    val uri = new URI(path)
    println(s"Resizing image $path to $size pixels")
    val resizeImageTask = ask(imageBrokerActor, GetImageRequest(uri, size)).mapTo[GetImageResponse]
    val response = Await.result(resizeImageTask, timeout.duration)
    println(s"Received resized image data ${response.data.length} bytes")
  }
}