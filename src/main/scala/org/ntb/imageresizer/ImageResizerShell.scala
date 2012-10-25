package org.ntb.imageresizer

import java.io.FileOutputStream
import java.net.URI
import java.net.URISyntaxException

import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.Await
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.duration.intToDurationInt
import akka.util.Timeout

import org.apache.http.HttpException
import org.ntb.imageresizer.actor.CachingImageBrokerActor
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.GetImageRequest
import org.ntb.imageresizer.actor.GetImageResponse
import org.ntb.imageresizer.actor.ResizeActor
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.util.FilePathUtils.getFilePath
import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.ByteStreams
import com.typesafe.config.ConfigFactory

object ImageResizerShell extends App {
  val config = ConfigFactory.parseString("""
    akka {
      event-handlers = ["akka.event.Logging$DefaultLogger"]
      loglevel = "INFO"
    }
  """)
  println("Starting ImageResizer shell")
  val resizeNodes = Runtime.getRuntime().availableProcessors()
  println("Deploying " + resizeNodes + " resizers")
  val system = ActorSystem("ImageResizer", ConfigFactory.load(config))
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(resizeNodes)), "resizer")
  val downloadActor = system.actorOf(Props[DownloadActor], "downloader")
  val imageBrokerActor = system.actorOf(Props[CachingImageBrokerActor], "imagebroker")
  processCommands()

  def processCommands() {
    try {
      while (true) {
        Console.print("> ")
        val command = Console.readLine()
        val tokens = command.split(' ').toList
        handleCommand(tokens)
      }
    } catch {
      case e: Exception =>
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
      case "resize" :: path :: rest =>
        try {
          val size = if (!rest.isEmpty) rest.head.toInt else 128
          handleResizeCommand(path, size)
        } catch {
          case e: NumberFormatException => Console.err.println("Invalid argument for size: '%s'".format(rest.mkString(" ")))
          case e: URISyntaxException => Console.err.println("Invalid URL: '%s'".format(path))
          case e: UnsupportedImageFormatException => Console.err.println("Unsupported image format for " + path)
          case e: HttpException => Console.err.println("Could not download '%s': %s".format(path, e.getMessage()))
        }
      case "exit" :: Nil =>
        exit()
      case command @ _ =>
        Console.err.println("Invalid command or arguments: '%s'".format(command.mkString(" ")))
    }
  }

  def handleResizeCommand(path: String, size: Int) {
    implicit val timeout = Timeout(10 seconds)
    val uri = new URI(path)
    println("Resizing image %s to %d pixels...".format(path, size))
    val resizeImageTask = ask(imageBrokerActor, GetImageRequest(uri, size)).mapTo[GetImageResponse]
    val response = Await.result(resizeImageTask, timeout.duration)
    println("Received resized image data, %d bytes".format(response.data.length))
    val filePath = getFilePath(uri).getOrElse(path)
    using(new FileOutputStream(filePath)) { output =>
      val bytesWritten = ByteStreams.copy(new ByteStringInputStream(response.data), output)
      println("Wrote image data to " + filePath + "(" + bytesWritten + " bytes)")
    }
  }
}