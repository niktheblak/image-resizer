package org.ntb.imageresizer.actor

import java.io.IOException

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize.ImageResizer.resizeImage
import org.ntb.imageresizer.resize.UnsupportedImageFormatException

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.util.ByteString
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import javax.imageio.ImageIO

case class ResizeImageRequest(data: ByteString, size: Int, format: ImageFormat)
case class ResizeImageResponse(data: ByteString)

class ResizeActor extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case ResizeImageRequest(data, size, format) =>
      log.debug("Received ResizeImageRequest([%d bytes], %d, \"%s\")".format(data.length, size, format))
      try {
        val resized = resizeImage(data, size, format)
        log.info("Image resized successfully, replying with ResizeImageResponse([%d bytes])".format(resized.length))
        sender ! ResizeImageResponse(resized)
      } catch {
        case e: UnsupportedImageFormatException =>
          sender ! Status.Failure(e)
        case e: IOException =>
          sender ! Status.Failure(e)
          throw e
      }
  }

  override def preStart() {
    ImageIO.setUseCache(false)
  }
}