package org.ntb.imageresizer.actor

import java.io.File

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.util.ByteString
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import javax.imageio.ImageIO

class ResizeActor extends Actor with ActorLogging {
  import context.dispatcher
  import ResizeActor._
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case ResizeImageRequest(data, size, format) =>
      try {
        val resized = resizeImage(data, size, format)
        log.debug("Image resized successfully, replying with ResizeImageResponse([%d bytes])".format(resized.length))
        sender ! ResizeImageResponse(resized)
      } catch {
        case e: UnsupportedImageFormatException =>
          sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
    case ResizeImageToFileRequest(source, target, size, format) =>
      try {
        resizeImage(source, target, size, format)
        log.debug("Image resized successfully, replying with ResizeImageToFileResponse()")
        sender ! ResizeImageToFileResponse(target.length())
      } catch {
        case e: UnsupportedImageFormatException =>
          sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }

  override def preStart() {
    ImageIO.setUseCache(false)
  }
}

object ResizeActor {
  case class ResizeImageRequest(data: ByteString, size: Int, format: ImageFormat)
  case class ResizeImageResponse(data: ByteString)
  case class ResizeImageToFileRequest(source: File, target: File, size: Int, format: ImageFormat)
  case class ResizeImageToFileResponse(fileSize: Long)
}