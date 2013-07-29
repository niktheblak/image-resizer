package org.ntb.imageresizer

import actor.file.{ResizeActor, FileCacheImageBrokerActor, DownloadActor}
import com.typesafe.config.ConfigFactory
import org.apache.http.HttpException
import FileCacheImageBrokerActor._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import java.net.URI
import java.net.URISyntaxException
import language.postfixOps

object ImageResizerShell extends App {
  implicit val timeout = Timeout(10 seconds)
  val config = ConfigFactory.parseString("""
    akka {
      akka.loggers = ["akka.event.Logging$DefaultLogger"]
      loglevel = "INFO"
    }
  """)
  println("Starting ImageResizer shell")
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  println("Deploying " + resizeNodes + " resizers")
  val system = ActorSystem("ImageResizer", ConfigFactory.load(config))
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(resizeNodes)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBrokerActor = system.actorOf(Props(classOf[FileCacheImageBrokerActor], downloadActor, resizeActor))
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
          val size = if (!rest.isEmpty) rest.head.toInt else 128
          handleResizeCommand(path, size)
        } catch {
          case e: NumberFormatException ⇒ Console.err.println(s"Invalid argument for size: ${rest.mkString(" ")}")
          case e: URISyntaxException ⇒ Console.err.println(s"Invalid URL: $path}")
          case e: UnsupportedImageFormatException ⇒ Console.err.println(s"Unsupported image format for $path")
          case e: HttpException ⇒ Console.err.println(s"Could not download $path: ${e.getMessage}")
        }
      case "clear" :: Nil ⇒
        imageBrokerActor ! ClearCache()
      case "exit" :: Nil ⇒
        exit()
      case command @ _ ⇒
        Console.err.println(s"Invalid command or arguments: ${command.mkString(" ")}")
    }
  }

  def handleResizeCommand(path: String, size: Int) {
    val uri = new URI(path)
    println("Resizing image %s to %d pixels...".format(path, size))
    val resizeImageTask = ask(imageBrokerActor, GetImageRequest(uri, size)).mapTo[GetImageResponse]
    val response = Await.result(resizeImageTask, timeout.duration)
    println("Received resized image data %d bytes to %s".format(response.data.length, response.data.getAbsolutePath))
  }
}