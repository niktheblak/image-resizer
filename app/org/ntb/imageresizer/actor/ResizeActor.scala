package org.ntb.imageresizer.actor

import akka.actor.Actor
import akka.util.{ ByteString, ByteStringBuilder }
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize.Resizer
import org.ntb.imageresizer.resize.UnsupportedImageFormatException

class ResizeActor extends Actor with ActorUtils with Resizer {
  import org.ntb.imageresizer.actor.ResizeActor._

  def receive = {
    case ResizeImageRequest(source, size, format) =>
      actorTry(sender()) {
        val input = source.iterator.asInputStream
        val builder = new ByteStringBuilder
        resizeImage(input, builder.asOutputStream, size, format)
        sender ! ResizeImageResponse(builder.result())
      } actorCatch {
        case e: UnsupportedImageFormatException =>
      }
  }
}

object ResizeActor {
  case class ResizeImageRequest(source: ByteString, size: Int, format: ImageFormat)
  case class ResizeImageResponse(data: ByteString)
}
