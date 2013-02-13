package org.ntb.imageresizer.actor

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize.Resizer._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.util.Timeout
import scala.concurrent.duration._
import java.io.File
import language.postfixOps

class ResizeActor extends Actor with ActorLogging {
  import ResizeActor._
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case ResizeImageRequest(source, target, size, format) =>
      try {
        resizeImage(source, target, size, format)
        log.debug("Image resized successfully, replying with ResizeImageResponse()")
        sender ! ResizeImageResponse(target.length())
      } catch {
        case e: UnsupportedImageFormatException =>
          sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }
}

object ResizeActor {
  case class ResizeImageRequest(source: File, target: File, size: Int, format: ImageFormat)
  case class ResizeImageResponse(fileSize: Long)
}