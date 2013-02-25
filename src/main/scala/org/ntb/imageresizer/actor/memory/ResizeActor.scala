package org.ntb.imageresizer.actor.memory

import akka.actor.Actor
import akka.util.ByteString
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.io.{ByteStringOutputStream, ByteStringInputStream}
import org.ntb.imageresizer.resize.Resizer._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.util.Loans.using

class ResizeActor extends Actor with ActorUtils {
  import ResizeActor._

  def receive = {
    case ResizeImageRequest(source, size, format) ⇒
      actorTry(sender) {
        using (new ByteStringInputStream(source)) { input =>
          using (new ByteStringOutputStream()) { output =>
            resizeImage(input, output, size, format)
            sender ! ResizeImageResponse(output.toByteString())
          }
        }
      } actorCatch {
        case e: UnsupportedImageFormatException ⇒
      }
  }
}

object ResizeActor {
  case class ResizeImageRequest(source: ByteString, size: Int, format: ImageFormat)
  case class ResizeImageResponse(data: ByteString)
}
